var MyQuiz = function (element) {
    
    if (!(element instanceof jQuery)) {
        element = $(element);
    }
    this._wrapper = element;
    
    
    this._quizBody = $("<div>",{'class' : 'k2i-quiz-body'});
    this._quizTable = $("<table>", {'class': 'k2i-quiz-table'});
    var quizTableRow1 = $("<tr>");
    var quizTableRow2 = $("<tr>");
    var quizTableRow1Cell1 = $("<td>");
    var quizTableRow2Cell1 = $("<td>");
    this._quizTable.append(quizTableRow1);
    this._quizTable.append(quizTableRow2);
    quizTableRow1.append(quizTableRow1Cell1);
    quizTableRow2.append(quizTableRow2Cell1);
    
    
    this._questionTitle = $("<div>",{'class':'k2i-quiz-title'});
    this._questionCountdown = $("<div>",{'class':'k2i-quiz-countdown'});
    this._questionContent = $("<div>",{'class':''});
    
    quizTableRow1Cell1.append(this._questionTitle);
    quizTableRow1Cell1.append(this._questionCountdown);
    quizTableRow2Cell1.append(this._questionContent);
    
    this._wrapper.append(this._quizBody);
    
    // By default hide the countdown.
    this._questionCountdown.hide();
    
    
    this._questions = [];
    this._activeQuestionIndex = -1;
    this._activeQuestion;
    
};

MyQuiz.prototype.addQuestion = function(question) {
    this._questions.push(question);
};

MyQuiz.prototype.goNextQuestion = function() {
    this._activeQuestionIndex++;
    
    if (this._activeQuestionIndex >= this._questions.length) {
        this.displayQuizFinalMessage();
    } else {
        this.start(this._activeQuestionIndex);    
    }
    
};


MyQuiz.prototype.start = function(questionIndex) {
    
    // If no index is specified, presume first;
    if (!questionIndex) {
        questionIndex = 0;   
    }
    
    var userId = this.getURLParameterByName("userId");
    
    $.ajax({
        type: "GET",
        url: "./AnswerService?"+$.param({
            actionVal: "canStart",
            userId: userId
        }),
        contentType: "application/json; charset=utf-8",
        dataType: "json",
        success: function (data1) {
            if (data1.error) {
                alert(data1.message);
            } else {
                if (data1.canStart) {
                    this.startQuestion(questionIndex);
                } else {
                    this.displayWaitMessage(data1.timeout);
                }
            }
        }.bind(this),
        error: function (request, status, errorThrown) {
            alert("Server error:"+status);
            this.stop();
        }.bind(this)
    });
    
}

MyQuiz.prototype.startQuestion = function(questionIndex) {
    
    // Fetch the active question.
    this._activeQuestionIndex = questionIndex;
    this._activeQuestion = this._questions[questionIndex];
    
    // Clear any previous config of the question
    this._activeQuestion.clear();
    
    // Register the callbacks.
    this._activeQuestion.onCorrectAnswer(function () {
        this.goNextQuestion();
    }.bind(this));
    this._activeQuestion.onIncorrectAnswer(function () {
        this.stop();
    }.bind(this));
    
    // Display the question.
    this._questionContent.empty().append(this._activeQuestion.getContent());
    this._questionTitle.text("Vraag "+(questionIndex+1)+"/4");
    this._quizBody.empty().append(this._quizTable);
    this._activeQuestion.start();
};


MyQuiz.prototype.displayQuizFinalMessage = function() {
	
	var title = $("<span>", {"class" : "k2i-title"}).text("Is Nice!");
	
	var paragraph = $("<p>",{"class": "k2i-intro"});
	var succesMessage = $("<div>", {"class" : "k2i-message"}).text("Het is gelukt! Verwacht binnenkort een uitnodiging op feesboek!");
	var partyMessage = $("<div>", {"class" : "k2i-message"}).text("And see you at the party!");
    var backImg = $("<img>", {"src" :"./img/back.gif"});
    
    
    paragraph.append(succesMessage);
    paragraph.append(backImg);
    paragraph.append(partyMessage);
    
    this._quizBody.empty();
    this._quizBody.append(title);
    this._quizBody.append(paragraph);
}

MyQuiz.prototype.displayWaitMessage = function(seconds) {
    
	var title = $("<span>", {"class" : "k2i-title"}).text("Spijtig!");
	var paragraph = $("<p>",{"class": "k2i-intro"});
	var waitMessage = $("<div>", {"class" : "k2i-message"})
	var blockedImg = $("<img>", {"src" :"./img/blocked.gif"});
	
    var waitMessageLeft = $("<span>").text("Verkeerd geantwoord! Je moet nog ");;
    var waitMessageSeconds = $("<span>").text(seconds);;
    var waitMessageRight = $("<span>").text("s wachten voor je nog eens mag proberen ...");;
    
    waitMessage.append(waitMessageLeft);
    waitMessage.append(waitMessageSeconds);
    waitMessage.append(waitMessageRight);
    
    paragraph.append(waitMessage);
    paragraph.append(blockedImg);
    
    this._quizBody.empty();
    this._quizBody.append(title);
    this._quizBody.append(paragraph);
    
    
    this._waitTimeoutStart = new Date().getTime();
    this._waitTimeoutStop = this._waitTimeoutStart + seconds*1000;
    this._waitCountDownInterval = setInterval(function () {
        
        var now = new Date().getTime();
        
        // Check if time has been reached ....
        if (now > this._waitTimeoutStop) {
            // Stop updating this after time is up.
            clearInterval(this._waitCountDownInterval);
            
            // Redirect to homepage again.
            location.reload();
        } else {
            var secLeft = Math.floor((this._waitTimeoutStop - now)/1000);
            waitMessageSeconds.text(secLeft);
        }
        
    }.bind(this), 500);
    
    
}

MyQuiz.prototype.stop = function () {
    // Ironically stopping the quiz will come down to restarting him again ...
    this.start();
}

MyQuiz.prototype.enableTimer = function() {
    this._questionCountdown.show();
}

MyQuiz.prototype.disableTimer = function() {
    this._questionCountdown.hide();
}

MyQuiz.prototype.updateTimer = function(msLeft) {
    var secLeft = Math.round(msLeft/1000);
    this._questionCountdown.text(secLeft+"s.");
}

MyQuiz.prototype.getJQElement = function() {
    return this._wrapper;
}

MyQuiz.prototype.getURLParameterByName = function(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}
