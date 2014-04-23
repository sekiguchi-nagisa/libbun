// @main
if(main){
	main();
}

// @extend
var __extends = this.__extends || function (d, b) {
	for (var p in b) if (b.hasOwnProperty(p)) d[p] = b[p];
	function __() { this.constructor = d; }
	__.prototype = b.prototype;
	d.prototype = new __();
}

// @Fault
var Fault = (function () {
    function Fault(message) {
        this.message = message;
        this.name = "Fault";
    }
    return Fault;
})();

// @SoftwareFault;@Fault;@extend
var SoftwareFault = (function (_super) {
    __extends(SoftwareFault, _super);
    function SoftwareFault(message) {
        _super.call(this, message);
        this.name = "SoftwareFault";
    }
    return SoftwareFault;
})(Fault);

// @catch;@SoftwareFault
function libbun_catch(e){
	return e;
}

// @strnull
function libbun_strnull(s){
	return s ? s : "null";
}

// @arraysize
def libbun_arraysize(a, n, v):
	while len(a) < n: a.append(v)
	while len(a) > n: a.pop()

// @mapget;@SoftwareFault
function libbun_mapget(m, k, v){
    if(k in m) return m[k];
    if(v) return v;
    throw new SoftwareFault("key not found: " + k);
}

// @arrayget;@SoftwareFault
function libbun_arrayget(a, i){
	if(i >= 0 && i < a.length) return a[i];
	throw new SoftwareFault("out of index: " + i);
}

// @arrayset;@SoftwareFault
function libbun_arrayset(a, i, v){
    if(i >= 0 && i < a.length){
        a[i] = v;
    }else{
        throw new SoftwareFault("out of index: " + i);
    }
}

// @intdiv
function libbun_intdiv(a, b){
	if(b == 0) throw new SoftwareFault("zero division");
	return Math.floor(a / b);
}
