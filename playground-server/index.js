///<reference path='./typings/jquery/jquery.d.ts' />

var Playground;
(function (Playground) {
    Playground.CodeGenTarget = "js";
    Playground.ParserTarget = "syntax::bun";

    function CreateEditor(query) {
        var editor = ace.edit(query);
        editor.setTheme("ace/theme/xcode");
        editor.getSession().setMode("ace/mode/javascript");
        return editor;
    }
    Playground.CreateEditor = CreateEditor;

    function ChangeSyntaxHighlight(editor, targetMode) {
        editor.getSession().setMode("ace/mode/" + targetMode);
    }
    Playground.ChangeSyntaxHighlight = ChangeSyntaxHighlight;
})(Playground || (Playground = {}));

var Debug = {};

$(function () {
    var zenEditor = Playground.CreateEditor("zen-editor");
    Debug.zenEditor = zenEditor;
    Playground.ChangeSyntaxHighlight(zenEditor, "typescript");
    var outputViewer = Playground.CreateEditor("output-viewer");
    Debug.outputViewer = outputViewer;
    outputViewer.setReadOnly(true);

    var GetSample = function (sampleName) {
        $.ajax({
            type: "GET",
            url: "/samples/" + sampleName + ".bun",
            success: function (res) {
                zenEditor.setValue(res);
                zenEditor.clearSelection();
            },
            error: function () {
                console.log("error");
            }
        });
    };

    var GenerateServer = function () {
        $.ajax({
            type: "POST",
            url: "/compile",
            data: JSON.stringify({ source: zenEditor.getValue(), target: Playground.CodeGenTarget, parser: Playground.ParserTarget }),
            dataType: 'json',
            contentType: "application/json; charset=utf-8",
            success: function (res) {
                outputViewer.setValue(res.source);
                outputViewer.clearSelection();
            },
            error: function () {
                console.log("error");
            }
        });
    };

    var timer = null;
    zenEditor.on("change", function (cm, obj) {
        if (timer) {
            clearTimeout(timer);
            timer = null;
        }
        timer = setTimeout(GenerateServer, 400);
    });

    var TargetNames = ["C", "CommonLisp", "C Sharp", "Java", "JavaScript", "LLVM", "Python", "R"];
    var TargetOptions = ["c", "cl", "cs", "java", "js", "ll", "py", "r"];
    var TargetMode = ["c_cpp", "lisp", "csharp", "java", "javascript", "assembly_x86", "python", "r"];

    var ParserNames = ["Bun", "Python"];
    var ParserOptions = ["syntax::bun", "syntax::python"];
    var ParserMode = ["typescript", "python"];

    var bind = function (n) {
        var Target = $('#Target-' + TargetNames[n]);
        Target.click(function () {
            Playground.CodeGenTarget = TargetOptions[n];
            $('li.active').removeClass("active");
            Target.parent().addClass("active");
            $('#active-lang').text(TargetNames[n]);
            $('#active-lang').append('<b class="caret"></b>');
            Playground.ChangeSyntaxHighlight(outputViewer, TargetMode[n]);
            if (timer) {
                clearTimeout(timer);
                timer = null;
            }
            GenerateServer();
        });
    };

    for (var i = 0; i < TargetNames.length; i++) {
        $("#Targets").append('<li id="Target-' + TargetNames[i] + '-li"><a href="#" id="Target-' + TargetNames[i] + '">' + TargetNames[i] + '</a></li>');
        bind(i);
    }

    var Samples = ["HelloWorld", "BinaryTrees", "Fibonacci", "NGram"];

    var sample_bind = function (n) {
        $('#sample-' + Samples[n]).click(function () {
            GetSample(Samples[n]);
        });
    };

    for (var i = 0; i < Samples.length; i++) {
        $("#zen-sample").append('<li id="sample-' + Samples[i] + '-li"><a href="#" id="sample-' + Samples[i] + '">' + Samples[i] + '</a></li>');
        sample_bind(i);
    }

    var parser_bind = function (n) {
        var Target = $('#Parser-' + ParserNames[n]);
        Target.click(function () {
            Playground.ParserTarget = ParserOptions[n];
            $('#parse-lang').text(ParserNames[n]);
            $('#parse-lang').append('<b class="caret"></b>');
            Playground.ChangeSyntaxHighlight(zenEditor, ParserMode[n]);
            if (timer) {
                clearTimeout(timer);
                timer = null;
            }
            GenerateServer();
        });
    };

    for (var i = 0; i < ParserNames.length; i++) {
        $("#Parser").append('<li id="Parser-' + ParserNames[i] + '-li"><a href="#" id="Parser-' + ParserNames[i] + '">' + ParserNames[i] + '</a></li>');
        parser_bind(i);
    }

    $("#Target-JavaScript-li").addClass("active");
    GenerateServer();
});
