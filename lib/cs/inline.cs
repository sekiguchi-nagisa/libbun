
// @main
namespace LibBunGenerated {
   public static partial class LibBunMain {
      public static void Main(String[] a) {
         main();
      }
   }
}

// @Fault;LibBun
namespace LibBun {
    public class Fault : SystemException {
        public Fault(string message) : base(message){
        }
    }
}

// @SoftwareFault;@Fault
namespace LibBun {
    public class SoftwareFault : Fault {
        public SoftwareFault(string message) : base(message){
        }
    }
}

// @Random;LibBun
namespace LibBun {
	public static partial class Lib {
		private static Random random;
		
		public static double Random(){
			if(random == null){
				random = new Random();
			}
			return random.NextDouble();
		}

		public static double Random(object dummy){
			return Lib.Random();
		}
	}
}

// @Error;@SoftwareFault
namespace LibBun {
	public static partial class Lib {
		public static object Error(string message){
			throw new SoftwareFault(message);
		}
	}
}

// @Throw;@SoftwareFault
namespace LibBun {
	public static partial class Lib {
		public static void Throw(Fault fault){
			throw fault;
		}
		public static void Throw(object obj){
			throw new SoftwareFault(obj.ToString());
		}
	}
}

// @ArrayPop;@LibBun
namespace LibBun {
    public class Lib : Fault {
        public static T ArrayPop(List<T> array) {
        	var ret = array[array.Count - 1];
        	array.RemoveAt(array.Count - 1);
        	return ret;
        }
    }
}
