var MyQuestion = function(id, title, quiz, maxTime) {
    
    // Properties
    this._id = id;
    this._title = title;
    this._maxTime = maxTime;
    this._startTime = -1;
    this._quiz = quiz;
    
    // The callbacks
    this._onIncorrectAnswer = [];
    this._onCorrectAnswer = [];   
}

MyQuestion.prototype.getContent = function() {
    // must be overriden by subclasses.   
}

MyQuestion.prototype.getAnswer = function() {
    // must be overriden by subclasses.
}

MyQuestion.prototype.focus = function() {
    // must be overriden by subclasses.
}

MyQuestion.prototype.start = function () {
    
    var userId = this.getURLParameterByName("userId");
    
    this._startTime = new Date().getTime();
    
    // If a maxtime is set, autosubmit the answer after this._maxTime
    if (this._maxTime > 0) {
        // Autosubmit the answer.
    	this._autoSubmitTimer = setTimeout(function () {
            this.answer(this.getAnswer());
        }.bind(this),this._maxTime)
        
        // Enable and update the timer each second.
        this._quiz.enableTimer();
        this._countdownInterval = setInterval(function() {
            var timeLeft = this._maxTime - (new Date().getTime() - this._startTime);
            this._quiz.updateTimer(timeLeft);
        }.bind(this), 500);
    } else {
        // No maxtime set, disable timer.
        this._quiz.disableTimer();
    }
    
    // Submit that the question is started to the backend.
    $.ajax({
            type: "GET",
            url: "./AnswerService?"+$.param({
                actionVal: "startQuestion",
                questionId: this._id,
                userId: userId
            }),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function (data1) {
                if (data1.error) {
                    alert(data1.message);
                    this.stop();
                } else {
                    // All OK, do nothing.   
                }
            }.bind(this),
            error: function (request, status, errorThrown) {
                alert("Server error:"+status);
                this.stop();
            }.bind(this)
        });
    
    this.focus();
}

MyQuestion.prototype.answer = function (answer) {
    
    var userId = this.getURLParameterByName("userId");
    
    // Stop the countdown (when set)
    if(this._countdownInterval) {
        clearInterval(this._countdownInterval);
    }
    if(this._autoSubmitTimer) {
    	clearInterval(this._autoSubmitTimer);
    }
    
    // Submit the answer to the server.
    $.ajax({
            type: "GET",
            url: "./AnswerService?"+$.param({
                actionVal: "answerQuestion",
                questionId: this._id,
                userId: userId,
                answer: answer
            }),
            contentType: "application/json; charset=utf-8",
            dataType: "json",
            success: function (data1) {
                if (data1.error) {
                    alert(data1.message);
                    this.stop();
                } else if (data1.answerCorrect) {
                    // Run all onCorrectAnswer callbacks.
                    $.each(this._onCorrectAnswer, function (index, callback) {
                        callback();
                    });
                } else {
                    // Run all onIncorrectAnswer callbacks.
                    $.each(this._onIncorrectAnswer, function (index, callback) {
                        callback();
                    });
                }
            }.bind(this),
            error: function (request, status, errorThrown) {
                alert("Server error:"+status);
                this.stop();
            }.bind(this)
        });
}

MyQuestion.prototype.stop = function () {
    // Stop the countdown (when set)
    if(this._countdownInterval) {
        clearInterval(this._countdownInterval);
    }
    // Stop autosubmit
    if (this._autoSubmit) {
        clearTimeout(this._autoSubmit);
    }
    // Stop the quiz.
    this._quiz.stop();
};

MyQuestion.prototype.clear = function() {
    this._onCorrectAnswer = [];
    this._onIncorrectAnswer = [];
}

MyQuestion.prototype.onCorrectAnswer = function(callback) {
    this._onCorrectAnswer.push(callback);   
}

MyQuestion.prototype.onIncorrectAnswer = function(callback) {
    this._onIncorrectAnswer.push(callback);
}

MyQuestion.prototype.getURLParameterByName = function(name) {
    name = name.replace(/[\[]/, "\\[").replace(/[\]]/, "\\]");
    var regex = new RegExp("[\\?&]" + name + "=([^&#]*)"),
        results = regex.exec(location.search);
    return results === null ? "" : decodeURIComponent(results[1].replace(/\+/g, " "));
}
