package name.sxli.qrel;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import name.dxliu.agent.OracleAgent;
import name.dxliu.associations.AssociationFinder;
import name.dxliu.associations.AssociationTree;
import name.dxliu.associations.PruningStrategy;
import name.dxliu.example.ExampleGraphAgent;

/**
 * The CertQR+ algorithm.
 */
public class KwdQueryRelaxCertQRP extends AbstractKwdRelaxAssociation {
	
	
	public KwdQueryRelaxCertQRP() {
	}
	
	public KwdQueryRelaxCertQRP(int diameter) {
		this.diameter = diameter;
	}

	@Override
	public AssociationTree getRelaxedAssociation(ExampleGraphAgent graphAgent, OracleAgent oracleAgent,
			List<List<Integer>> groupQuery) {
		
		int kwdNum = groupQuery.size();
		
		List<Integer> maxQuery = getMaxQuery(kwdNum, groupQuery, graphAgent, oracleAgent);

		if(maxQuery == null || maxQuery.size() == 0)
			return null;
		
		hitwords = "";
		hitNumber = 0;
		boolean[] isHit = new boolean[groupQuery.size()];
		for(int entity : maxQuery) {
			for(int i=0; i<groupQuery.size(); i++) {
				if(!isHit[i] && groupQuery.get(i).contains(entity)) {
					hitwords += i + " ";
					isHit[i] = true;
					hitNumber++;
				}	
			}
		}
		hitEntity = "";
		for(int entity : maxQuery)
			hitEntity += entity + ",";
		
		AssociationFinder finder=new AssociationFinder();
		finder.resetRecorder();
		finder.setRuningStrategy(PruningStrategy.BSC);
		finder.associationLimit = 1;
		List<AssociationTree> results = finder.discovery(graphAgent, oracleAgent, diameter, maxQuery);
		
		return results.get(0);
	}
	
	private List<Integer> getMaxQuery(int kwdNum, List<List<Integer>> groupQuery, 
			ExampleGraphAgent graphAgent, OracleAgent oracleAgent) {
		Map<Integer, List<Integer>> entity2kwdInds = new HashMap<>();
		for(int i=0; i<kwdNum; i++) {
			for(int entity : groupQuery.get(i)) {
				List<Integer> kwdInds = entity2kwdInds.getOrDefault(entity, new ArrayList<>());
				kwdInds.add(i);
				entity2kwdInds.put(entity, kwdInds);
			}
		}
		
		List<List<Integer>> combineEntities = new ArrayList<>();
		double combineNum = 1;
		for(int i=0; i<kwdNum; i++) {
			combineEntities.add(groupQuery.get(i));
			combineNum *= groupQuery.get(i).size();
		}
		List<Integer> maxQuery = new ArrayList<>();
		int maxHit = 0;
		double cnt = 0;
		int[] ind = new int[kwdNum];
		while(cnt < combineNum) {
			List<Integer> group = new ArrayList<>();
			for(int i=0; i<kwdNum; i++)
				group.add(combineEntities.get(i).get(ind[i]));
			Set<Integer> set = new HashSet<>(group); // remove duplicate
			int[] queryEntities = new int[set.size()];
			Iterator<Integer> iter = set.iterator();
			int ii = 0;
			while(iter.hasNext())
				queryEntities[ii++] = iter.next();
			
			QueryRelaxationCertQRdgs dgs = new QueryRelaxationCertQRdgs();
			int[] relaxEntities = null;
			try {
				// The CertQR+ algorithm using two heuristics.
				relaxEntities = dgs.relaxQuery(graphAgent, oracleAgent, diameter, queryEntities);
			} catch (Exception e) {
				e.printStackTrace();
			}
			List<Integer> relaxQuery = new ArrayList<>();
			for(int re : relaxEntities)
				relaxQuery.add(re);
			Set<Integer> hitKwdInds = new HashSet<>();
			for(int e : relaxQuery)
				hitKwdInds.addAll(entity2kwdInds.get(e));
			if(relaxQuery != null && hitKwdInds.size() > maxHit) {
				int hitNumber = 0;
				boolean[] isHit = new boolean[groupQuery.size()];
				for(int entity : relaxQuery) {
					for(int i=0; i<groupQuery.size(); i++) {
						if(!isHit[i] && groupQuery.get(i).contains(entity)) {
							isHit[i] = true;
							hitNumber++;
						}	
					}
				}
				if(hitNumber > maxHit) {
					maxQuery = relaxQuery;
					maxHit = hitNumber;
				}
				if(maxHit == kwdNum)
					break;
			}
			cnt++;
			if(cnt >= combineNum)
				break;
			int changeInd = kwdNum - 1;
			while(ind[changeInd] + 1 == combineEntities.get(changeInd).size())
				changeInd--;
			ind[changeInd]++;
			for(int i=changeInd+1; i<kwdNum; i++)
				ind[i] = 0;
		}

		if(maxQuery.size() == 0) {
			List<Entry<Integer, List<Integer>>> list = new ArrayList
					<Entry<Integer, List<Integer>>>(entity2kwdInds.entrySet());
			Collections.sort(list, new Comparator<Map.Entry<Integer, List<Integer>>>() {
	            public int compare(Entry<Integer, List<Integer>> o1, Entry<Integer, List<Integer>> o2) {
	                return o2.getValue().size() - o1.getValue().size();
	            }
	        });
			maxQuery.add(list.get(0).getKey()); 
		}
		return maxQuery;
	}
	
}
