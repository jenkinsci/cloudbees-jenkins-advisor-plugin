function validateOnStart(checkUrl,email,password) {
    var parameters = [email,password];
    
      var spinner = document.getElementById("loadSpinnerDiv");
      var target = document.getElementById("loadMessageDiv");;
      spinner.style.display="block";
      target.innerHTML = "";
    
      new Ajax.Request(checkUrl, {
          parameters: parameters,
          onComplete: function(rsp) {
              spinner.style.display="none";
              applyErrorMessage(target, rsp);
              layoutUpdateCallback.call();
              var s = rsp.getResponseHeader("script");
              try {
                  geval(s);
              } catch(e) {
                  window.alert("failed to evaluate "+s+"\n"+e.message);
              }
          }
    });
}