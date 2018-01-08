var ImageQuestion = function (id, title, quiz, question, imageUrl, time) {
    
    MyQuestion.call(this, id, title, quiz, time);
    this._imageUrl = imageUrl;
    this._question = question;
}

// OOP inheritance in JS
ImageQuestion.prototype = Object.create(MyQuestion.prototype);
ImageQuestion.prototype.constructor = ImageQuestion;

ImageQuestion.prototype.getContent = function() {
    
    this._questionTable = $("<table>", {'class': 'k2i-question-table'});
    var questionTableRow1 = $("<tr>");
    var questionTableRow2 = $("<tr>");
    var questionTableRow1Cell1 = $("<td>");
    var questionTableRow2Cell1 = $("<td>");
    
    this._questionTable.append(questionTableRow1);
    this._questionTable.append(questionTableRow2);
    questionTableRow1.append(questionTableRow1Cell1);
    questionTableRow2.append(questionTableRow2Cell1);
    
    
    var picImage = $("<img>",{'src' : this._imageUrl, 'class' : 'k2i-question-image'});
    this._answerInput = $("<input>",{'type':'text', 'class' : 'k2i-question-input', 'placeholder' : this._question});
    
    // When enter is pressed, submit answer.
    this._answerInput.keyup(function(e) {
    	if(e.keyCode == 13){
    		this.answer(this.getAnswer());
    	}
    }.bind(this));
    
    questionTableRow2Cell1.append(picImage);
    questionTableRow1Cell1.append(this._answerInput);
    
    return this._questionTable;
}

ImageQuestion.prototype.getAnswer = function() {
    return this._answerInput.val();
}

ImageQuestion.prototype.focus = function() {
    this._answerInput.focus();
}