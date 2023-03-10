package gilp.query;

import gilp.rdf3x.RDFSubGraph;
import gilp.rdf3x.RDFSubGraphSet;
import gilp.rdf3x.Triple;

import gilp.rules.Clause;
import gilp.rules.JoinType;
import gilp.rules.RDFPredicate;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;

/*
 * A simple query engine which can support conjunctive queries 
 * over RDF data set.
 * Currently we only support at most two triple patterns. 
 * CJC Nov. 12 2015
 * */
public class SimpleCNFQueryEngine implements QueryEngine {
	ArrayList<Triple> _dataset = null;
	
	public void setDataSet(ArrayList<Triple> data){
		this._dataset = new ArrayList<>();
		this._dataset.addAll(data);
	}

	@Override
	public Triple getTripleByTid(String tid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesBySubject(String subject) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getTriplesByPredicate(String predicate) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ArrayList<Triple> getAllTriples() {
		return this._dataset;
	}
	 
	@Override
	/*retrieve RDF triples by a conjunctive query in the format of a Clause.*/
	public RDFSubGraphSet getTriplesByCNF(Clause cls){
		//TODO remove comparators here 
		Clause clsWithoutComparators = cls.clone();
		ArrayList<RDFPredicate> comparators = new ArrayList<>();
		ArrayList<RDFPredicate> myIter1 = clsWithoutComparators.getIterator();
		Iterator<RDFPredicate> myIter = myIter1.iterator();
		while(myIter.hasNext()){
			RDFPredicate tp = myIter.next();
			if(tp.isPredicateComparator()){
				comparators.add(tp.clone());
				myIter.remove();
			}
		}	
		
		if (clsWithoutComparators.getBodyLength()==0){
			System.out.println(this.getClass().getName() + ":ERROR! The body of the clause is empty1");
			return null;
		}
		else if (clsWithoutComparators.getBodyLength()==1){

					ArrayList<RDFPredicate> myIter2 =clsWithoutComparators.getIterator();
			Iterator<RDFPredicate> iter = myIter2.iterator();
			RDFPredicate tp =iter.next();
			ArrayList<Triple> listTriples = selectTriplesByPattern(tp);	 
			return buildSubGraphSet(listTriples, tp);
		}
		else{
			ArrayList<RDFPredicate> myIter3 = clsWithoutComparators.getIterator();
			ArrayList<RDFPredicate> triple_patterns = new ArrayList<RDFPredicate>();
			for (RDFPredicate tp : myIter3) {
				triple_patterns.add(tp.clone());
			}			
			 
			//record whether a pattern is joined
			boolean[] joined = new boolean[triple_patterns.size()];
			for(int i=0; i<joined.length; i++)
				joined[i] = false;
		
			//add the first triple pattern into the results
			ArrayList<Triple> temp_list = selectTriplesByPattern(triple_patterns.get(0));
			RDFSubGraphSet rlts =  buildSubGraphSet(temp_list,triple_patterns.get(0));
			joined[0] = true;
			
			int num = joined.length - 1;
				//num is the number of triple patterns which have not been joined 
			while(num>0){
				boolean findJoin = false;
				for (int i=0;i<triple_patterns.size();i++){
					if (joined[i])//the i^th triple pattern has been joined
						continue;
					RDFPredicate tp = triple_patterns.get(i);
					temp_list = selectTriplesByPattern(tp);
					RDFSubGraphSet inter_rlts = join(rlts, temp_list, tp);
						//try to join the i^th trple pattern
					if (inter_rlts!=null){
						rlts = inter_rlts;
						findJoin = true;
						joined[i] = true;
						num--;
						break;
					}					
				}
				if (!findJoin)
					break;				
			} 
			if (num>0)//there exist some triple patterns cannot be joined, hence no result
				return null;
			else{ 
				int nc = comparators.size();
				if (nc>0){
					ArrayList<RDFSubGraph> matchedSGs = new ArrayList<>();
					for (RDFSubGraph sg: rlts.getSubGraphs()){
						boolean matched = false;
						for(RDFPredicate cmpTP : comparators){
							boolean findSub = false, findObj = false;
							String sub="", obj=""; 
							
							//if an argument of the comparator is a constant, we do not need to find the matched assignment in the SG
							if(!cmpTP.isSubjectVariable()){
								sub = cmpTP.getSubject();
								findSub = true;
							}
							if (!cmpTP.isObjectVariable()){
								obj = cmpTP.getObject();
								findObj = true;
							}
							
							//at least one argument in the comparator should be a variable
							ArrayList<RDFPredicate>		myIter4 = clsWithoutComparators.getIterator();
							int i = 0;
							for (RDFPredicate tp : myIter4) {
								Triple crntTriple = sg.getTriples().get(i++);
								if (!findSub) {
									if (tp.getSubject().equals(cmpTP.getSubject())) {
										sub = crntTriple.get_subject();
										findSub = true;
									} else if (tp.getObject().equals(cmpTP.getSubject())) {
										sub = crntTriple.get_obj();
										findSub = true;
									}
								}
								if (!findObj) {
									if (tp.getSubject().equals(cmpTP.getObject())) {
										obj = crntTriple.get_subject();
										findObj = true;
									} else if (tp.getObject().equals(cmpTP.getObject())) {
										obj = crntTriple.get_obj();
										findObj = true;
									}
								}

								if (findSub && findObj) {
									matched = true;
									break;
								}
							}
							
							if (matched && RDFPredicate.satisfyComparison(cmpTP.getPredicateName(), sub, obj)){
								matchedSGs.add(sg.clone());
							}							
						}//for(RDFPredicate cmpTP : comparators)
					}//for (RDFSubGraph sg: rlts.getSubGraphs())		
					rlts.getSubGraphs().clear();
					rlts.getSubGraphs().addAll(matchedSGs);
				}
				return rlts;
			}
		}		 
	}
	 
	
	//transform a list of triples to a RDFSubGraphSet
	private RDFSubGraphSet buildSubGraphSet(ArrayList<Triple> triples, RDFPredicate tp){
		RDFSubGraphSet rlts = new RDFSubGraphSet();			
		rlts.getPredicates().add(tp);
		for (Triple t : triples){
			RDFSubGraph sg =new RDFSubGraph();
			sg.addTriple(t);
			rlts.addSubGraph(sg);
		}
		return rlts;
	}
	 
	
	/*
	 * select from data-set all the triples that satisfy the given triple pattern.
	 * */
	private ArrayList<Triple> selectTriplesByPattern(RDFPredicate tp){
		ArrayList<Triple> rlts = new ArrayList<Triple>();
		for (Triple tr: this._dataset){
			if (tp.match(tr))
				rlts.add(tr.clone());
		}
		return rlts;
	} 
	
	private class JoinCondition{
		int _pattern_idx; 
		JoinType[] _joins; 
		
		JoinCondition(int tp_idx, JoinType[] joins){
			this._pattern_idx = tp_idx;
			this._joins = new JoinType[joins.length];
			System.arraycopy(joins,0, this._joins, 0, joins.length);
		}
	}
	/*
	 * join between a set of sub_graphs and a set of triples
	 * the result is new sub_graph set. 
	 * return null if the two inputs cannot be joined
	 * */
	private RDFSubGraphSet join(RDFSubGraphSet sub_graphs, 	
			ArrayList<Triple> triples, RDFPredicate triple_pattern){
		//find a predicate pattern in sub_graphs which can be joined with the pattern of triples
		
		

	//	RDFPredicate tp = null;
		JoinType[] joins = null;
		HashSet<RDFPredicate> preds = sub_graphs.getPredicates();
		ArrayList<JoinCondition> joinConditions = new ArrayList<JoinCondition>();
		int i = 0;
		for(RDFPredicate tp :preds){
		//for (i=0;i<preds.size();i++){
		//	tp = preds.get(i);
			joins =  RDFPredicate.getJoinTypes(tp, triple_pattern);
			if (joins!=null){
				JoinCondition jc = new JoinCondition(i, joins);
				joinConditions.add(jc);
			}
			i++;
		}
		 
		if (joinConditions.isEmpty())
			return null;

		// build a new sug_graph set with one more predicate
		RDFSubGraphSet sg_set = new RDFSubGraphSet();
		preds = new HashSet<RDFPredicate>();
		preds.addAll(sub_graphs.getPredicates());
		preds.add(triple_pattern);
		sg_set.setPredicates(preds);

		// for each sub-graph, try to math with each triple
		for (RDFSubGraph sg : sub_graphs.getSubGraphs()) {
			for (Triple t : triples) {
				//for each triple t, there could be multiple triple patterns in the sub_graph which can join t				
				boolean joinable = true;
				
				for (JoinCondition jc: joinConditions){
					int join_tp_idx = jc._pattern_idx;
					int join_pos1 = 1, join_pos2=1;
					
					//two triples may have at most four join types simultaneously, all join requirements should be met such that 
					// the join is success
					for (JoinType jt: jc._joins){
						switch (jt){
						case SS: 
							join_pos1 = join_pos2 =1;
							break;
						case SO:
							join_pos1 = 1; join_pos2 = 2;
							break;
						case OS:
							join_pos1 = 2; join_pos2 = 1;
							break;
						case OO:
							join_pos1 = 2; join_pos2 = 2; 
							break;
						}
						if (!sg.match(t, join_tp_idx, join_pos1, join_pos2)) {
							joinable = false;
							break;							
						}
					}//for (JoinType jt: jc._joins)
					
				}//for (JoinCondition jc: joinConditions)
				
				if (joinable){
					RDFSubGraph new_sg = sg.clone();
					new_sg.addTriple(t.clone());
					sg_set.addSubGraph(new_sg);
				}
				
			}//for (Triple t : triples)
		}//for (RDFSubGraph sg : sub_graphs.getSubGraphs()) 
				
		 
		if (sg_set.getSubGraphs().size()==0)
			return null;
		else
			return sg_set;
	} 
	//###############################################################################

	//                   unit  tests
			
	//###############################################################################
	
	static void testComparators(){
		ArrayList<Triple> dataset = new ArrayList<Triple>();
		Triple t = new Triple("Deng_Xiaoping","hasChild","Deng_Pufang");
		dataset.add(t);
		t = new Triple("Deng_Xiaoping","hasGivenName","Deng");
		dataset.add(t);
		t = new Triple("Deng_Pufang","hasGivenName","Deng");
		dataset.add(t);
		 
		
		Clause cls = new Clause();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?p1"));
		tp.setPredicateName("hasChild");
		tp.setObject(new String("?p2"));
		cls.addPredicate(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?p1"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?n1"));
		cls.addPredicate(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?p2"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?n2"));
		cls.addPredicate(tp);
		
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?n1"));
		tp.setPredicateName("equalTo");
		tp.setObject(new String("?n2"));
		cls.addPredicate(tp);
		
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(dataset);
		
		RDFSubGraphSet rlt = sqe.getTriplesByCNF(cls);
		if (rlt != null) {
			for (RDFSubGraph twig : rlt.getSubGraphs()) {
				System.out.println(twig.toString());
			}
		} else {
			System.out.println("Empty results set.");
		}
	}
	
	static void testNumeric(){
		ArrayList<Triple> dataset = new ArrayList<Triple>();
		Triple t = new Triple("China","hasArea","900");
		dataset.add(t);
		t = new Triple("USA","hasArea","-10");
		dataset.add(t);
		t = new Triple("China","hasGDP","100000");
		dataset.add(t);
		t = new Triple("USA","hasGDP","200000");
		dataset.add(t);
		
		Clause cls = new Clause();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasArea");
		tp.setObject(new String("(-90,10000)"));
		cls.addPredicate(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGDP");
		tp.setObject(new String("?o1"));
		cls.addPredicate(tp);
		
		
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(dataset);
		
		RDFSubGraphSet rlt = sqe.getTriplesByCNF(cls);
		if (rlt != null) {
			for (RDFSubGraph twig : rlt.getSubGraphs()) {
				System.out.println(twig.toString());
			}
		} else {
			System.out.println("Empty results set.");
		}
	}
	
	static void basicTest(){
		ArrayList<Triple> dataset = new ArrayList<Triple>();
		Triple t = new Triple("Yao_Ming","hasGivenName","Yao");
		dataset.add(t);
		t = new Triple("Yao_Ming","hasNationality","China");
		dataset.add(t);
		t = new Triple("Yao_Chen","hasGivenName","Yao");
		dataset.add(t);
		t = new Triple("Yao_Chen","hasNationality","China");
		dataset.add(t);
		t = new Triple("Yao_William","hasGivenName","Yao");
		dataset.add(t);
		t = new Triple("Yao_William","hasNationality","US");
		dataset.add(t);
		t = new Triple("Yao_Ming","hasGender","Male");
		dataset.add(t);
		t = new Triple("Yao_William","hasGender","Male");
		dataset.add(t);
		t = new Triple("Yao_Chen","hasGender","Female");
		dataset.add(t);
		t = new Triple("China","locatesIn","Asia");
		dataset.add(t);
		t = new Triple("US","locatesIn","America");
		dataset.add(t);
 		
	
		Clause cls = new Clause();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o1"));
		cls.addPredicate(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasNationality");
		tp.setObject(new String("?o2"));		
		cls.addPredicate(tp);
		
		tp = new RDFPredicate();
		tp.setSubject(new String("?s1"));
		tp.setPredicateName("hasGender");
		tp.setObject(new String("Male"));		
		cls.addPredicate(tp);
		
	
		SimpleCNFQueryEngine sqe = new SimpleCNFQueryEngine();
		sqe.setDataSet(dataset);
		
		RDFSubGraphSet rlt = sqe.getTriplesByCNF(cls);
		if (rlt != null) {
			for (RDFSubGraph twig : rlt.getSubGraphs()) {
				System.out.println(twig.toString());
			}
		} else {
			System.out.println("Empty results set.");
		}	
		
	}
	
	public static void main(String[] args){
		testComparators();
	}

}

/*
 * 					int joinPosition1 = 1, joinPosition2 = 1;
					if (tempRlt1.getPattern().getObject().equals(joinCondition)){
						joinPosition1 = 2;
					}
					if (tempRlt2.getPattern().getObject().equals(joinCondition)){
						joinPosition2 = 2;
					}
					//TODO we may need to handle the case like <?a R ?b> AND <?b Q ?a>

					joinedRlt = join(tempRlt1, joinPosition1, tempRlt2, joinPosition2);

 * */
/*
 * compute the join between two sets of triples. 
 * each input list should have an identical predicate.
 * @joinPosOfList1 the join position of list1: 1 means subject and 2 means object
 * @joinPosOfList2 the join position of list2: 1 means subject and 2 means object
 * the joined result of list1 is stored in the first element of the output array.
 * and 	the joined result of list2 is stored in the second element of the output array.
 * return null if no joined result can be found.
 * 
private RDFSubGraphSet join(ArrayList<Triple> list1, int joinPosOfList1,RDFPredicate tp1,
		ArrayList<Triple> list2,  int joinPosOfList2,  RDFPredicate tp2){
	RDFTwigSet rlt = new RDFTwigSet(tp1, tp2);	 
	
	//the most simple strategy: nest join
	for (Triple tr1: list1){
		for (Triple tr2: list2){
			String str1 = joinPosOfList1==1?tr1.get_subject():tr1.get_obj();
			String str2 = joinPosOfList2==1?tr2.get_subject():tr2.get_obj();
			if (str1.equals(str2)){
				 rlt.addTwig(tr1, tr2);
			}
		}
	}
	
	if (rlt.getSubGraphs().size()>0)
		return rlt;
	else 
		return null;
}*/
/*
 * //get a list of triples for each triple pattern in the clause.
		Iterator<Predicate> myIter = cls.getIterator();
		ArrayList<IntermediateResults> listInterRlts = new ArrayList<IntermediateResults>();
		while(myIter.hasNext()){
			RDFPredicate tp = (RDFPredicate)myIter.next();
			IntermediateResults tempList = selectTriplesByPattern(tp);
			if (tempList!=null){
				if(tempList._triples.size()>0)
					listInterRlts.add(tempList);
			}
		}
		
		IntermediateResults matchedList = null;
		
		//create a working list which contains the first element of listInterRlts in the beginning
		ArrayList<IntermediateResults> listWorking = new ArrayList<IntermediateResults>();
		listWorking.add(listInterRlts.get(0));
		listInterRlts.remove(0);
		
	 	while (listInterRlts.size()>0){
	 		//choose a pair <l1, l2> of lists which can be joined
			for (IntermediateResults  tempRlt1 : listWorking){
				for (IntermediateResults tempRlt2 : listInterRlts){
					if (tempRlt1 != tempRlt2){
						String joinCondition = getJoinCondition(tempRlt1.getPattern(),tempRlt2.getPattern());					
						if (joinCondition == null)
							continue;
				  		
						listWorking = join(listWorking, tempRlt2);
							//join a new set of triples with the working list
		 
						matchedList = tempRlt2;//record which inter-result is used in join
						break;
					}
				}
			}
			 
			if (listWorking.size()==0){
				//no results
				return null;
			}
		
			//remove the joined intermediate results
			listInterRlts.remove(matchedList); 
		}
		
	 	if (listWorking.size()==0)
	 		return null;
	 	else{
	 		ArrayList<Triple> results = new ArrayList<Triple>();
	 		for (IntermediateResults  tempRlt1 : listWorking){
	 			results.addAll(tempRlt1.getTriples());
	 		}
	 		return results;
	 	}
 * */
/*
 *  compute the join between several lists and another list 
	private ArrayList<IntermediateResults> join(ArrayList<IntermediateResults> list1, 
			IntermediateResults list2){
		//list2 belongs to a single triple pattern; list1 may involve several patterns
		//try s of list2.pattern with list1 then try o of list2.pattern
		
		//TODO very hard!!! we need to consider other implementation approaches. 
		
		return null;
	}
 
 */
/*
 * private class IntermediateResults{
		ArrayList<Triple> _triples;
		RDFPredicate _pattern;
		
		IntermediateResults(ArrayList<Triple> triples, RDFPredicate pattern){
			this._triples = triples;
			this._pattern = pattern;
		}
		
		public ArrayList<Triple> getTriples() {
			return _triples;
		}
		 
		public RDFPredicate getPattern() {
			return _pattern;
		}
		 
		
	}
	
 */