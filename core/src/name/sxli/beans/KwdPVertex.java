package name.sxli.beans;

import java.util.Comparator;

public class KwdPVertex extends KwdVertex {
	public static Comparator<KwdPVertex> largerCmp=new Comparator<KwdPVertex>(){
		@Override
		public int compare(KwdPVertex v1, KwdPVertex v2) {
			if(v1.priority > v2.priority)
				return -1;
			else if(v1.priority < v2.priority)
				return 1;
			else
				return v1.vid - v2.vid;
		}
	};
	
	public static Comparator<KwdPVertex> smallerCmp=new Comparator<KwdPVertex>(){
		@Override
		public int compare(KwdPVertex v1, KwdPVertex v2) {
			if(v1.priority < v2.priority)
				return -1;
			else if(v1.priority > v2.priority)
				return 1;
			else
				return v1.vid - v2.vid;
		}
	};
	
	public double priority;
	
	public KwdPVertex() {}

	public KwdPVertex(int vid, int kwdind, int eind, double priority) {
		super(vid, kwdind, eind);
		this.priority = priority;
	}

	public KwdPVertex clone()
	{
		KwdPVertex v = new KwdPVertex(vid, kwdind, eind, priority);
		return v;
	}
}
