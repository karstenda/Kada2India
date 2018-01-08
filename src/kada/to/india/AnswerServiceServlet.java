package kada.to.india;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.labs.repackaged.org.json.JSONException;
import com.google.appengine.labs.repackaged.org.json.JSONObject;

import kada.to.india.data.Question;
import kada.to.india.data.Quiz;
import kada.to.india.data.User;
import kada.to.india.data.Users;

import java.util.logging.Level;
import java.util.logging.Logger;


@SuppressWarnings("serial")
public class AnswerServiceServlet extends HttpServlet {

	private Logger logger = Logger.getLogger(AnswerServiceServlet.class.toString());
	
	public static HashMap<String, Date> blacklist = new HashMap<String, Date>();

	private DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
	
	public void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws IOException {

		String action = req.getParameter("actionVal");
		String questionId = req.getParameter("questionId");
		String userId = req.getParameter("userId");

		Question question = Quiz.getQuestion(questionId);
		User user = Users.getUser(userId);

		// Do some checks
		if (action == null || action.trim().length() == 0) {
			sendError(resp, "No action defined in request");
			return;
		}
		if (user == null) {
			sendError(resp, "Sorry ik herken je niet ... gebruik je wel de volledige URL die ik je gegeven heb?");
			return;
		}

		// Start processing the actions ....
		logger.log(Level.INFO, "Processing action '"+action+"' for user "+user.getId());
		
		if (action.equalsIgnoreCase("canStart")) {

			// This action checks if the user is allowed to start the quiz.

			Date blacklistDate = blacklist.get(user.getId());
			boolean canNotStart = blacklistDate != null
					&& blacklistDate.after(new Date());
			try {
				JSONObject object = new JSONObject();
				object.put("error", false);
				object.put("canStart", !canNotStart);
				if (canNotStart) {
					long timeout = blacklistDate.getTime()
							- (new Date()).getTime();
					object.put("timeout", (long) Math.floor(timeout / 1000));
				}
				resp.setContentType("application/json");
				resp.getWriter().println(object.toString());
				return;
			} catch (JSONException e) {
				e.printStackTrace();
				resp.sendError(500);
				return;
			}

		}

		// Other actions can only be executed if the user is not blacklisted, so
		// check that now.
		Date blacklistDate = blacklist.get(user.getId());
		if (blacklistDate != null && blacklistDate.after(new Date())) {
			long timeout = blacklistDate.getTime() - (new Date()).getTime();
			timeout = (long) Math.floor(timeout / 1000);
			sendError(resp, "You have to wait " + timeout
					+ "s before trying again.");
			return;
		}

		if (action.equalsIgnoreCase("completeQuiz")) {

			
			
		}

		// The following actions can only be executed when a question is
		// specified, so check that now.
		if (question == null) {
			sendError(resp, "No valid question was defined.");
			return;
		}

		// Check if it's one of the other actions ...
		if (action.equalsIgnoreCase("startQuestion")) {

			req.getSession().setAttribute("question_" + questionId + "_start",
					new Date());

			// Return a JSON indicating that no error occured.
			try {
				JSONObject object = new JSONObject();
				object.put("error", false);
				resp.setContentType("application/json");
				resp.getWriter().println(object.toString());
			} catch (JSONException e) {
				e.printStackTrace();
				resp.sendError(500);
				return;
			}

		} else if (action.equalsIgnoreCase("answerQuestion")) {

			// Extract the answer.
			String answer = req.getParameter("answer");
			if (answer == null || answer.trim().length() == 0) {
				answer = "";
			}

			// Check if we have to check that nobody cheated the front end.
			if (question.getMaxTime() > 0) {
				try {
					Date start = (Date) req.getSession().getAttribute(
							"question_" + questionId + "_start");
					Date now = new Date();
					// Check if not too much time passed since starting the
					// question (allow for 3000ms delay).
					if (now.getTime() - start.getTime() - 3000 > question
							.getMaxTime()) {
						sendError(
								resp,
								"Cheatig detected, question was answered too late, this incident will be reported to Karsten, Foei! ("+userId+")",
								true);
						blockFromTestFor(user, 60 * 24); // block him for a day.
						markCheater(userId); // Mark him in the DB.
						return;
					}
				} catch (ClassCastException e) {
					sendError(
							resp,
							"Cheatig detected, question was not yet started, this incident will be reported to Karsten, Foei! ("+userId+")",
							true);
					blockFromTestFor(user, 60 * 24); // block him for a day.
					markCheater(userId); // Mark him in the DB.
					return;
				}
			}

			// Check the answer
			if (question.isCorrectAnswer(answer)) {

				logger.log(Level.INFO, "User '"+userId+"' solved question "+question.getId());
				
				// Save in the session that a correct answer was given.
				req.getSession().setAttribute(
						"question_" + questionId + "_completed", true);
				
				// Check if all questions are correctly answered now.
				this.checkAllAnswersCorrect(req.getSession(), userId);
				
				try {
					JSONObject object = new JSONObject();
					object.put("error", false);
					object.put("answerCorrect", true);
					resp.setContentType("application/json");
					resp.getWriter().println(object.toString());
				} catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(500);
					return;
				}
			} else {
				
				logger.log(Level.INFO, "User '"+userId+"' failed question "+question.getId());
				
				blockFromTestFor(user, 2); // block him for 3 minutes.
				
				// Mark the failed attempt of this user in the DB.
				markFailedAttempt(user.getId());
				
				try {
					JSONObject object = new JSONObject();
					object.put("error", false);
					object.put("answerCorrect", false);
					resp.setContentType("application/json");
					resp.getWriter().println(object.toString());
				} catch (JSONException e) {
					e.printStackTrace();
					resp.sendError(500);
					return;
				}
			}

		}
	}

	private void checkAllAnswersCorrect(HttpSession session, String userId) {
		
		logger.log(Level.INFO, "Checking wether user: "+userId+" solved everything!");
		
		for (Question question: Quiz.getQuestions()) {
			Boolean answer = (Boolean) session.getAttribute("question_" + question.getId() + "_completed");
			if (answer == null || !answer) {
				logger.log(Level.INFO, "User: "+userId+" did NOT solve everything!");
				return;
			}
		}
		// If we get here, the user did complete all questions.
		logger.log(Level.INFO, "User: "+userId+" did solve everything!");
		
		// Set this in the DB
		Entity userRef;
		try {
			userRef = datastore.get(KeyFactory.createKey("UserRef", userId));
		} catch (EntityNotFoundException e) {
			userRef = new Entity("UserRef", userId);
			userRef.setProperty("invited", false);
			userRef.setProperty("completed", false);
			userRef.setProperty("completedDate", null);
			userRef.setProperty("attempts", 1);
			userRef.setProperty("cheating", false);
		}
		userRef.setProperty("completed", true);
		userRef.setProperty("completedDate", new Date());
		datastore.put(userRef);
		
		// Send an email
		sendEmail("User completed", "User completed quiz with id: "+userId);
	}
	
	private void blockFromTestFor(User user, long minutes) {
		Date now = new Date();
		long timeout = minutes * 60 * 1000;
		blacklist.put(user.getId(), new Date(now.getTime() + timeout));
		logger.log(Level.WARNING, "Blacklisting user: "+user.getId()+" for "+minutes+" minutes.");
	}
	
	private void markFailedAttempt(String userId) {
		// Set this in the DB
		Entity userRef;
		try {
			userRef = datastore.get(KeyFactory.createKey("UserRef", userId));
		} catch (EntityNotFoundException e) {
			userRef = new Entity("UserRef", userId);
			userRef.setProperty("invited", false);
			userRef.setProperty("completed", false);
			userRef.setProperty("completedDate", null);
			userRef.setProperty("attempts", 1);
			userRef.setProperty("cheating", false);
			datastore.put(userRef);
			return;
		} 
		
		// increment the attempts
		long attempts = 0;
		if (userRef.getProperty("attempts") != null) {
			attempts = (Long) userRef.getProperty("attempts");
		}
		userRef.setProperty("attempts", attempts+1);
		datastore.put(userRef);
		
	}

	private void markCheater (String userId) {
		// Set this in the DB
		Entity userRef;
		try {
			userRef = datastore.get(KeyFactory.createKey("UserRef", userId));
		} catch (EntityNotFoundException e) {
			userRef = new Entity("UserRef", userId);
			userRef.setProperty("invited", false);
			userRef.setProperty("completed", false);
			userRef.setProperty("completedDate", null);
			userRef.setProperty("attempts", 1);
			userRef.setProperty("cheating", false);
		}
		userRef.setProperty("cheating", true);
		datastore.put(userRef);
	}
	
	private void sendError(HttpServletResponse resp, String message)
			throws IOException {
		sendError(resp, message, false);
	}

	private void sendError(HttpServletResponse resp, String message,
			boolean dueToCheating) throws IOException {
		
		// Log this ...
		logger.log(Level.SEVERE, "Error happened: "+message);
		
		if (dueToCheating) {
			// Send email that cheater was found ...
			sendEmail("Cheater foud", message);
		}
		
		try {
			JSONObject object = new JSONObject();
			object.put("error", true);
			object.put("message", message);
			object.put("cheating", dueToCheating);
			resp.setContentType("application/json");
			resp.getWriter().println(object.toString());
		} catch (JSONException e) {
			e.printStackTrace();
			resp.sendError(500);
			return;
		}
		
		
	}
	
	private void sendEmail(String subject, String msgBody) {
		
		logger.log(Level.INFO, "Sending email ("+subject+"): "+msgBody);
		
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

        try {
            Message msg = new MimeMessage(session);
            msg.setFrom(new InternetAddress("answerservice@kada2india.appspotmail.com"));
            msg.addRecipient(Message.RecipientType.TO, new InternetAddress("karsten.daemen@gmail.com", "Karsten"));
            msg.setSubject(subject);
            msg.setText(msgBody);
            Transport.send(msg);
            logger.log(Level.INFO, "Mail has been sent");
        } catch (AddressException e) {
        	logger.log(Level.SEVERE, e.getMessage());
            e.printStackTrace();
        } catch (MessagingException e) {
        	logger.log(Level.SEVERE, e.getMessage());
        	e.printStackTrace();
        } catch (UnsupportedEncodingException e) {
        	logger.log(Level.SEVERE, e.getMessage());
        	e.printStackTrace();
        }
		
	}
}
