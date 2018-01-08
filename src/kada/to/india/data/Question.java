package kada.to.india.data;

public class Question {

	
	private String id;
	private String title;
	private String[] answers;
	private long maxTime;
	
	public Question(String id, String title, long maxTime, String ... answers ) {
		
		this.id = id;
		this.title = title;
		this.answers = answers;
		this.maxTime = maxTime;	
	}
	
	public boolean isCorrectAnswer(String answer) {
		boolean retval = false;
		for (String correctAnswer: this.answers) { 
			if (correctAnswer.trim().equalsIgnoreCase(answer.trim())) {
				return true;
			}
		}
		return false;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String[] getAnswer() {
		return answers;
	}

	public void setAnswer(String[] answers) {
		this.answers = answers;
	}

	public long getMaxTime() {
		return maxTime;
	}

	public void setMaxTime(long maxTime) {
		this.maxTime = maxTime;
	}
	
	
}
