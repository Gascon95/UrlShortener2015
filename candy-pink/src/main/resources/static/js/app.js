$(document).ready(
    function() {
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                $.ajax({
                    type : "POST",
                    url : "/link",
                    data : $(this).serialize(),
                    success : function(msg) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + msg.uri
                            + "'>"
                            + msg.uri
                            + "</a></div>"
                            + "<div class='alert alert-success lead'>And your token is: "
                            + msg.token
                            + "</div>");
                    },
                    error : function() {
                        $("#result").html(
                                "<div class='alert alert-danger lead'>An error has ocurred</div>"
			);
                    }
                });
            });
    });
