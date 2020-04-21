package name.sxli.beans;

public class KwdVertex {
	
	public int vid;
	public int kwdind; //the keyword index
	public int eind; //the entity index
	
	public KwdVertex() {}

	public KwdVertex(int vid, int kwdind, int eind) {
		this.vid = vid;
		this.kwdind = kwdind;
		this.eind = eind;
	}

	public KwdVertex clone()
	{
		KwdVertex v = new KwdVertex(vid, kwdind, eind);
		return v;
	}
}
