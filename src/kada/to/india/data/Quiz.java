package kada.to.india.data;

import java.util.ArrayList;
import java.util.List;

public class Quiz {

	private static final List<Question> questions = new ArrayList<Question>();
	
	static {
		questions.add(new Question("1","GodQuestion1", 8000, "visnu", "vishnu", "vishnoe"));
		questions.add(new Question("2","TajMahalQuestion", 6000, "agra", "uttar pradesh"));
		questions.add(new Question("3","GodQuestion2", 6000, "ganes", "ganesh", "ganesha"));
		questions.add(new Question("4","MovieQuestion", 7000, "ajay devgn", "devgn", "ajay devgan"));
	}
	
	public static List<Question> getQuestions() {
		return questions;
	}
	
	public static Question getQuestion(String id) {
		for (Question question: questions) {
			if (question.getId().equals(id)) {
				return question;
			}
		}
		return null;
	}
	
	
}
