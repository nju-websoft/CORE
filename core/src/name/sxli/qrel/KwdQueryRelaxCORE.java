package name.sxli.qrel;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import name.dxliu.agent.OracleAgent;
import name.dxliu.associations.AssociationTree;
import name.dxliu.bean.IntegerEdge;
import name.dxliu.example.ExampleGraphAgent;
import name.sxli.beans.KwdPVertex;

/**
 * The CORE algorithm.
 */
public class KwdQueryRelaxCORE extends AbstractKwdRelaxAssociation {
	
	public KwdQueryRelaxCORE() {}
	
	public KwdQueryRelaxCORE(int diameter) {
		this.diameter = diameter;
	}

	@Override
	public AssociationTree getRelaxedAssociation(ExampleGraphAgent graphAgent, OracleAgent oracleAgent, List<List<Integer>> groupQuery) {
		List<Integer> Qopt = new ArrayList<>(); // may duplicate, namely one entity maps several keywords
		int kwdNum = groupQuery.size();
		Map<Integer, BitSet> visited = new HashMap<>(); //visited vertices from each starting vertex
		BitSet checked = new BitSet(vertexMax); //vertices checked by FindAns
		
		// Note that the priority is the upper bound of contained keywords number, which is contrary to dor in the paper.
		PriorityQueue<KwdPVertex> pq = new PriorityQueue<>(KwdPVertex.largerCmp);
		
		for(int i=0; i<kwdNum; i++) {
			for(int ii=0; ii<groupQuery.get(i).size(); ii++) {
				int qe1 = groupQuery.get(i).get(ii);
				if(visited.get(qe1) == null)
					visited.put(qe1, new BitSet(vertexMax));
				if(visited.get(qe1).get(qe1))
					continue;
				visited.get(qe1).set(qe1);
				double prio = 1;
				for(int j=0; j<kwdNum; j++) {
					if(j == i)
						continue;
					for(int qe2 : groupQuery.get(j)) {
						int dist = oracleAgent.queryDistance(qe1, qe2);
						if(dist <= diameter) {
							prio++;
							break;
						}
					}
				}
				Set<IntegerEdge> edges=graphAgent.graph.edgesOf(qe1);
				double divider = edges.size() + 2.0; //add a constant to avoid integer when degree=1
				prio += 1.0 / divider; //add the decimal part
				pq.add(new KwdPVertex(qe1, i, ii, prio));
			}
		}
		
		int pathDist = diameter / 2;
		KwdQueryRelSub subQuery = new KwdQueryRelSub();
		while(!pq.isEmpty()) {
			KwdPVertex cur = pq.poll();
			if(Math.floor(cur.priority) == 1 || Math.floor(cur.priority) <= Qopt.size()) // cannot be better
				break;
			
			if(!checked.get(cur.vid)) {
				List<Integer> Qcur = subQuery.OptWithCert(graphAgent, oracleAgent, diameter, groupQuery, cur.vid, Qopt);
				if(Qcur != null && Qcur.size() > Qopt.size())
					Qopt = Qcur;
				checked.set(cur.vid);
				
				int length = oracleAgent.queryDistance(cur.vid, groupQuery.get(cur.kwdind).get(cur.eind));
				if(length < pathDist) {
					if(Qopt.size() == Math.floor(cur.priority))
						break;
					List<int[]> neighborEdges = graphAgent.getNeighborInfo(cur.vid);
					for(int[] edge : neighborEdges){
						int v1 = edge[0];
						int length1 = oracleAgent.queryDistance(v1, groupQuery.get(cur.kwdind).get(cur.eind));
						if(!visited.get(groupQuery.get(cur.kwdind).get(cur.eind)).get(v1) && length1 == length+1){ //only explore a shortest path
							double prio1 = 1;
							for(int i=0; i<kwdNum; i++) {
								if(i == cur.kwdind)
									continue;
								for(int qe : groupQuery.get(i)) {
									int dist = length1 + oracleAgent.queryDistance(v1, qe);
									if(dist <= diameter) {
										prio1++;
										break;
									}
								}
							}
							visited.get(groupQuery.get(cur.kwdind).get(cur.eind)).set(v1);
							if(prio1 <= Qopt.size())
								continue;
							Set<IntegerEdge> edges=graphAgent.graph.edgesOf(v1);
							double divider = edges.size() + 2.0; //add a constant to avoid integer when degree=1
							prio1 += 1.0 / divider; //add the decimal part
							pq.add(new KwdPVertex(v1, cur.kwdind, cur.eind, prio1));
						}
					}
				}
			}
		}
		
		if(Qopt.size() == 0)
			return null;
		hitNumber = Qopt.size();
		Set<Integer> set = new HashSet<>(Qopt);
		hitEntity = "";
		for(int entity : set)
			hitEntity += entity + ",";
		
		hitwords = "";
		boolean[] isHit = new boolean[groupQuery.size()];
		for(int entity : set) {
			for(int i=0; i<groupQuery.size(); i++) {
				if(!isHit[i] && groupQuery.get(i).contains(entity)) {
					hitwords += i + " ";
					isHit[i] = true;
				}
			}
		}
		
		List<Integer> optQuery = new ArrayList<>(new HashSet<>(Qopt)); //remove duplicate
		AssociationTree association = subQuery.getRelaxAssociation(graphAgent, diameter, optQuery);
		return association;
	}

}
