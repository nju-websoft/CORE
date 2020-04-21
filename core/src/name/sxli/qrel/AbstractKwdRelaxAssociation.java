package name.sxli.qrel;

import java.util.List;

import name.dxliu.agent.OracleAgent;
import name.dxliu.associations.AssociationTree;
import name.dxliu.example.ExampleGraphAgent;

public abstract class AbstractKwdRelaxAssociation {
	
	public final int vertexMax = 6000000; //It depends on the number of vertex in the graph.If the number is larger than this value,please modify it.
	public int hitNumber = 0; //number of hit keywords
	public String hitwords = "";
	public String hitEntity = "";
	public int diameter = 0; //diameter of answer

	/**
	 * This method is to find compact and relaxable answers to keyword queries
	 * over knowledge graphs.
	 * 
	 * @param graphAgent
	 *            answer the neighborhood query.
	 * @param oracleAgent
	 *            answer the distance query.
	 * @param groupQuery
	 *            entity groups mapping keywords.
	 * @return
	 */
	public abstract AssociationTree getRelaxedAssociation(ExampleGraphAgent graphAgent, OracleAgent oracleAgent, List<List<Integer>> groupQuery);
}
