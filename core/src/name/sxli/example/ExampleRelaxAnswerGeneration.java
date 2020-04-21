package name.sxli.example;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jgrapht.graph.DirectedMultigraph;
import org.jgrapht.graph.SimpleGraph;

import name.dxliu.associations.AssociationNode;
import name.dxliu.associations.AssociationTree;
import name.dxliu.bean.IntegerEdge;
import name.dxliu.bean.SimpleEdge;
import name.dxliu.example.ExampleGraphAgent;
import name.dxliu.example.ExampleOracleAgent;
import name.dxliu.oracle.DefaultOracle;
import name.sxli.qrel.AbstractKwdRelaxAssociation;
import name.sxli.qrel.KwdQueryRelaxCORE;
import name.sxli.qrel.KwdQueryRelaxCertQRP;

/**
 * This class demonstrate how to run the getRelaxedAssociation algorithm.
 * The result is an association tree.
 * 
 */
public class ExampleRelaxAnswerGeneration {
	
	public static void main(String[] args) {
		Step1_PreprocessTriples();
		Step2_CreateOracle();
		Step3_CreateKeywordIndex();
		Step4_GetRelaxAssociation();
	}

	private static final String outDir = "example";
	private static int propertyRange;
	private static int entityRange;
	
	/**
	 * Map entity to id and store the dictionary.
	 */
	private static void Step1_PreprocessTriples() {
		List<String> allLine = null;
		try {
			allLine = Files.readAllLines(Paths.get("example/ExampleTriples"), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		Set<String> properties = new TreeSet<>();
		Set<String> entities = new TreeSet<>();
		
		for(String line:allLine){
			String[] triple = line.split(",");	
			entities.add(triple[0]);
			properties.add(triple[1]);
			entities.add(triple[2]);
		}
		List<String> vocabularies = new ArrayList<>();
		
		vocabularies.addAll(properties);
		String propertiesRange = "0-"+(vocabularies.size()-1);		
		propertyRange = vocabularies.size()-1;
		
		vocabularies.addAll(entities);
		String entitiesRange = (vocabularies.size()-entities.size())+"-"+ (vocabularies.size()-1);
		entityRange = vocabularies.size()-1;
		System.out.println("property range: "+propertiesRange+", entity range: "+entitiesRange);
		
		Map<String,Integer> dictionary = new TreeMap<>();
		for(int i = 0,len = vocabularies.size();i<len;i++){
			dictionary.put(vocabularies.get(i), i);
		}

		SimpleGraph< Integer, SimpleEdge> graph = new SimpleGraph<>(SimpleEdge.class);
		try {
			FileWriter fileWriter = new FileWriter(outDir + "/out_id_relation_triples");
			for(String line:allLine){
				String[] triple = line.split(",");
				fileWriter.write(dictionary.get(triple[0])+" "+dictionary.get(triple[1])+" "+dictionary.get(triple[2])+"\n");
				fileWriter.flush();		
				
				graph.addVertex(dictionary.get(triple[0]));
				graph.addVertex(dictionary.get(triple[2]));
				graph.addEdge(dictionary.get(triple[0]), dictionary.get(triple[2]));

			}
			fileWriter.close();
			
			fileWriter = new FileWriter(outDir + "/out_dict");
			for(int i = 0,len = vocabularies.size();i<len;i++){
				fileWriter.write(i+","+vocabularies.get(i)+"\n");
				fileWriter.flush();			
			}
			fileWriter.close();
			
			fileWriter = new FileWriter(outDir+"/out_undirected_graph");
			for(SimpleEdge edges : graph.edgeSet()){
				Integer source =  (Integer) edges.getSource();
				Integer target =  (Integer) edges.getTarget();
				fileWriter.write(source+" "+target+"\n");
				fileWriter.flush();	
			}
			fileWriter.close();
			
			fileWriter = new FileWriter(outDir+"/out_id_range");		
			fileWriter.write("property:"+0+"-"+propertyRange+"\n");
			fileWriter.write("entity:"+(propertyRange+1)+"-"+entityRange+"\n");
			fileWriter.flush();	
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Create oracle to answer the query of distance.
	 */
	private static void Step2_CreateOracle() {
		DefaultOracle oracle = new DefaultOracle();
		//construct
		oracle.ConstructIndex("example/out_undirected_graph");
		//store
		oracle.StoreIndex("example/oracle");
		//load
		oracle=new DefaultOracle();
		oracle.LoadIndex("example/oracle");
		//query
		// 11 for United Kingdom and 14 for Yellowstone National Park in example graph.
		int distance  = oracle.Query(11, 14);
		System.out.println("distance: "+distance);
	}
	
	/**
	 * Create lucene index to answer the query of keyword-to-entity mapping.
	 */
	private static void Step3_CreateKeywordIndex() {
		try {
			Directory dir = FSDirectory.open(Paths.get(outDir + "/kwdInd"));
			Analyzer luceneAnalyzer = new StandardAnalyzer();
			IndexWriterConfig iwc = new IndexWriterConfig(luceneAnalyzer);
			IndexWriter indexWriter = new IndexWriter(dir, iwc);
			indexWriter.deleteAll();
			
			List<String> allLine = Files.readAllLines(Paths.get(outDir + "/out_dict"), Charset.defaultCharset());
			for(int i=0; i<allLine.size(); i++){
				if(i <= propertyRange)
					continue;
				Document doc = new Document();
				String id = allLine.get(i).split(",")[0];
				String label = allLine.get(i).split(",")[1];
				TextField labelField = new TextField("label", label, Field.Store.YES);
				TextField idField = new TextField("id", id, Field.Store.YES);
				doc.add(labelField);
				doc.add(idField);
				indexWriter.addDocument(doc);
			}
			indexWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Running example of CORE and CertQR+.
	 */
	private static void Step4_GetRelaxAssociation() {
		DirectedMultigraph<Integer, IntegerEdge> graph = new DirectedMultigraph<>(IntegerEdge.class);
		List<String> allLine = null;
		try {
			allLine = Files.readAllLines(Paths.get("example/out_id_relation_triples"), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String line : allLine) {
			String[] spo = line.split(" ");
			Integer source = Integer.valueOf(spo[0]);
			Integer target = Integer.valueOf(spo[2]);
			graph.addVertex(source);
			graph.addVertex(target);
			graph.addEdge(source, target, new IntegerEdge(source, target, Integer.parseInt(spo[1])));
		}
		ExampleGraphAgent graphAgent = new ExampleGraphAgent(graph);
		ExampleOracleAgent oracleAgent = new ExampleOracleAgent();
		
		String[] keywords = "united states yellowstone park trip".split(" ");
    	List<List<Integer>> groupQuery = new ArrayList<>();
		for(String keyword : keywords) {
    		List<Integer> hitEntities = new ArrayList<>();
			try {
				Query query = new TermQuery(new Term("label", keyword));
				IndexReader indReader = DirectoryReader.open(FSDirectory.open(Paths.get(outDir + "/KwdInd")));
				IndexSearcher searcher = new IndexSearcher(indReader);
		        ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;
				for (int jj = 0; jj < hits.length; jj++) {
					Document doc = searcher.doc(hits[jj].doc);
					int id = Integer.parseInt(doc.get("id"));
					hitEntities.add(id);
				}
				System.out.println(keyword + " " + hitEntities);
				groupQuery.add(hitEntities);
			} catch (IOException e) {
				e.printStackTrace();
			}
    	}
		
		int diameter = 2;
		//CORE
		AbstractKwdRelaxAssociation core = new KwdQueryRelaxCORE(diameter);
		AssociationTree result = core.getRelaxedAssociation(graphAgent, oracleAgent, groupQuery);
		System.out.println("--------CORE--------");
		printTree(result);
		
		//CertQR+
		AbstractKwdRelaxAssociation certqrp = new KwdQueryRelaxCertQRP(diameter);
		result = certqrp.getRelaxedAssociation(graphAgent, oracleAgent, groupQuery);
		System.out.println("-------CertQR+-------");
		printTree(result);
	}
	
	private static void printTree(AssociationTree tree) {
		Map<Integer, String> dict = readDictionary();
		Iterator<AssociationNode> iterator = tree.getDFSIterator();		
		while(iterator.hasNext()) {
			AssociationNode node = iterator.next();
			if(node.getFather() == null)
				continue;
			
			int subjectId, predicateId, objectId;
			if(node.getRelation()<0){
				subjectId = node.getId();
				predicateId = -node.getRelation();
				objectId = node.getFather().getId();				
			}else{
				subjectId =  node.getFather().getId();
				predicateId = node.getRelation();
				objectId = node.getId();			
			}
			System.out.println(dict.get(subjectId)+" —— "+dict.get(predicateId)
				+" —— "+dict.get(objectId));
		}
	}
	
	private static Map<Integer, String> readDictionary(){
		Map<Integer, String> dictionary = new TreeMap<>();
		List<String> allLine = null;
		try {
			allLine = Files.readAllLines(Paths.get(outDir + "/out_dict"), Charset.defaultCharset());
		} catch (IOException e) {
			e.printStackTrace();
		}
		for (String line : allLine) {
			String[] spo = line.split(",");
			Integer id = Integer.valueOf(spo[0]);
			String label = spo[1];
			dictionary.put(id, label);
		}
		return dictionary;
	}
	
}
