package name.sxli.qrel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import name.dxliu.agent.GraphAgent;
import name.dxliu.agent.OracleAgent;
import name.dxliu.associations.AssociationTree;
import name.dxliu.util.TimeCounter;
import name.sxli.beans.Node;
import name.sxli.util.CombinationUtil;


public class KwdQueryRelSub {
	
	public final int vertexMax = 6000000;
	
	public int cert;
	public int cert1; //neighbor of cert, only useful when D is odd and |cov1|-|cov3|>1
	public int certRel;
	public boolean useCert1;
	public Set<Integer> cert1Entities; // entities satisfy dist(e,cert1)=ceil(D/2)-1
	

	/**
	 * This method corresponds to algorithm 2: FindAns. The difference is that
	 * the implementation first finds the largest sub-query, then combines the
	 * answer.
	 * 
	 * @param graphAgent
	 *            answer the neighborhood query.
	 * @param oracleAgent
	 *            answer the distance query.
	 * @param diameter
	 *            the diameter bound.
	 * @param groupQuery
	 *            entity groups mapping keywords.
	 * @param v
	 *            the certificate vertex to be checked.
	 * @param Qopt
	 *            a known optimal solution.
	 * @return an entity set that covers most keywords certified by v
	 */
	public List<Integer> OptWithCert(GraphAgent graphAgent,
			OracleAgent oracleAgent, int diameter, List<List<Integer>> groupQuery, int v, List<Integer> Qopt){
		
		int kwdNum = groupQuery.size();
		
		List<List<Integer>> U1=new ArrayList<>(kwdNum); //dist(u,v)<=ceil(D/2)
		List<List<Integer>> U2=new ArrayList<>(kwdNum); //dist(u,v)=ceil(D/2)
		List<List<Integer>> U3=new ArrayList<>(kwdNum); //dist(u,v)<ceil(D/2)
		for(int i=0; i<kwdNum; i++) {
			U1.add(new ArrayList<>());
			U2.add(new ArrayList<>());
			U3.add(new ArrayList<>());
		}
		BitSet cov1 = new BitSet(kwdNum); // {i: UW1.get(i).size()>0}
		BitSet cov2 = new BitSet(kwdNum); // {i: UW2.get(i).size()>0}
		BitSet cov3 = new BitSet(kwdNum); // {i: UW3.get(i).size()>0}
		
		int semipathLen = (diameter + 1) / 2;
		
		for(int i=0; i<kwdNum; i++) {
			for(int u : groupQuery.get(i)) {
				int dist = oracleAgent.queryDistance(u, v);
				if(dist < semipathLen) {
					U1.get(i).add(u);
					cov1.set(i);
					U3.get(i).add(u);
					cov3.set(i);
				} else if(dist == semipathLen) {
					U1.get(i).add(u);
					cov1.set(i);
					U2.get(i).add(u);
					cov2.set(i);
				}
			}
		}
		
		if(Qopt != null && cov1.cardinality() > Qopt.size()) {
			List<Integer> Qmax = new ArrayList<>();
			if(diameter % 2 == 0) {
				cert = v;
				for(int i=0; i<U1.size(); i++) {
					if(U1.get(i).size() == 0)
						continue;
					if(U1.get(i).contains(v)) //query entity may be certificate
						Qmax.add(v);
					else
						Qmax.add(U1.get(i).get(0));
				}
				return Qmax;
			}
			// D is odd start here
			if(cov3.cardinality() + 1 >= cov1.cardinality()) {
				cert = v;
				useCert1 = false;
				for(int i=0; i<U3.size(); i++) {
					if(U3.get(i).size() == 0)
						continue;
					if(U3.get(i).contains(v))
						Qmax.add(v);
					else
						Qmax.add(U3.get(i).get(0));
				}
				if(cov3.cardinality() < cov1.cardinality()) { // have one ceil(D/2)
					BitSet temp = (BitSet) cov1.clone();
					temp.xor(cov3);
					int ind = temp.nextSetBit(0);
					Qmax.add(U2.get(ind).get(0)); //U2 cannot include certificate
				}
				return Qmax;
			}
			// keyword entities all ceil(D/2) to v have intersection
			BitSet temp = (BitSet) cov1.clone();
			temp.xor(cov3);
			int ind = temp.nextSetBit(0);
			List<Integer> intersect = U2.get(ind); // intersection of those ceil(D/2) to v
			while(true) {
				ind = temp.nextSetBit(ind + 1);
				if(ind == -1)
					break;
				intersect.retainAll(U2.get(ind));
			}
			if(intersect.size() > 0) {
				cert = v;
				useCert1 = false;
				for(int i=0; i<U3.size(); i++) {
					if(U3.get(i).size() == 0)
						continue;
					if(U3.get(i).contains(v))
						Qmax.add(v);
					else
						Qmax.add(U3.get(i).get(0));
				}
				for(int i=0; i<cov1.cardinality()-cov3.cardinality(); i++)
					Qmax.add(intersect.get(0));
				return Qmax;
			}
			// condition 2, cert has a neighbor cert1
			List<int[]> neighborEdges = graphAgent.getNeighborInfo(v);
			int Nmax = 0;
			List<Integer> Qsmall = new ArrayList<>(); 
			for(int i=0; i<U3.size(); i++) {
				if(U3.get(i).size() == 0)
					continue;
				if(U3.get(i).contains(v))
					Qsmall.add(v);
				else
					Qsmall.add(U3.get(i).get(0));
			}
			for(int[] edge : neighborEdges){
				List<Integer> Qcur = new ArrayList<>(Qsmall);
				int v1 = edge[0];
				for(int i=0; i<U2.size(); i++){
					if(U2.get(i).size() == 0 || U3.get(i).size() > 0) //if U3 adds, U2 doesn't need to add
						continue;
					for(int u : U2.get(i)) {
						int dist = oracleAgent.queryDistance(v1, u);
						if(dist < semipathLen) { // v has a neighbor v1 that dist(u,v1)<=ceil(D/2)-1
							Qcur.add(u);
							break;
						}
					}
				}
				if(Qcur.size() > Nmax && Qcur.size() > Qopt.size()){
					Nmax = Qcur.size();
					Qmax = Qcur;
					cert = v;
					cert1 = v1;
					certRel = edge[1];
					useCert1 = true;
					cert1Entities = new HashSet<>();
					for(int i=cov3.cardinality(); i<Qcur.size(); i++)
						cert1Entities.add(Qcur.get(i));
				}
				if(Nmax == cov1.cardinality()) //find common vertex and no better sub-query
					return Qmax;
			}
			if(Qmax.size() > Qopt.size())
				return Qmax;
			return null;
		}
		else
			return null;
	}
	
	public AssociationTree getRelaxAssociation(GraphAgent graphAgent, int diameter, List<Integer> optQuery) {
		int N = optQuery.size();
		int pathLen = (diameter + 1) / 2;
		List<List<Node>> paths = new ArrayList<>(N);
		List<AssociationTree> result = new ArrayList<>();	
		Set<List<Integer>> canonicalCodeSet = new HashSet<>();
		TimeCounter tc = new TimeCounter();
		if(!useCert1) {
			for(int i=0; i<N; i++) {
				Node node = findShortestPath(graphAgent, optQuery.get(i), cert, pathLen);
				List<Node> path = new ArrayList<>();
				path.add(node);
				paths.add(path);
			}
			boolean coincide = true; // paths may overlap
			int father = paths.get(0).get(0).father.id;
			while(father != -1) {
				for(int i=1; i<N; i++) {
					int father2 = paths.get(i).get(0).father.id;
					if(father2 != father) {
						coincide = false;
						break;
					}
				}
				if(!coincide)
					break;
				for(int i=0; i<N; i++) {
					paths.get(i).set(0, paths.get(i).get(0).father);
				}
				father = paths.get(0).get(0).father.id;
			}
		} else {
			for(int i=0; i<N; i++) {
				Node node;
				if(cert1Entities.contains(optQuery.get(i))) {
					Node neighbor = findShortestPath(graphAgent, optQuery.get(i), cert1, pathLen - 1);
					node = new Node(cert, certRel, neighbor);
				}
				else {
					Node neighbor = findShortestPath(graphAgent, optQuery.get(i), cert1, pathLen - 2); //first find path to cert1, avoid loop
					if(neighbor != null)
						node = new Node(cert, certRel, neighbor);
					else
						node = findShortestPath(graphAgent, optQuery.get(i), cert, pathLen - 1);
				}
				List<Node> path = new ArrayList<>();
				path.add(node);
				paths.add(path);
			}
		}
		CombinationUtil.CombinateAndGenerateResult(1, tc, result, canonicalCodeSet, paths, diameter, optQuery);
		return result.get(0);
	}
	
	/**
	 * This method is to find shortest path between two entities.
	 * @param graphAgent answer the neighbor info
	 * @param source entity 1
	 * @param target entity 2
	 * @param pathLen length bound of the path
	 * @return
	 */
	private Node findShortestPath(GraphAgent graphAgent, int source, int target, int pathLen) {
		BitSet sourceVisit = new BitSet(vertexMax);
		List<Node> iterateSrc = new ArrayList<>();
		Node srcNode = new Node(source, -1, new Node(-1));
		if(source == target)
			return srcNode;
		iterateSrc.add(srcNode);
		sourceVisit.set(source);
		for(int curPathLen=1; curPathLen<=pathLen; curPathLen++) {
			List<Node> sourceTempList = new ArrayList<>();
			for(Node curNode : iterateSrc) {
				int curNodeId = curNode.id;
				List<int[]> allEdges = graphAgent.getNeighborInfo(curNodeId);
				for(int[] ie : allEdges) {
					int neighborId = ie[0];
					int relation = ie[1];
					if(sourceVisit.get(neighborId))
						continue;
					sourceVisit.set(neighborId);
					Node neighborNode = new Node(neighborId, relation, curNode);
					if(neighborId == target) {
						return neighborNode;
					}
					sourceTempList.add(neighborNode);
				}
			}
			iterateSrc = sourceTempList;
		}
		return null;
	}
}
