package gilp.query;

import gilp.comments.Comment;
import gilp.comments.Feedback;
import gilp.knowledgeClean.GILPSettings;
import gilp.rules.RulePackage;
import gilp.rdf3x.RDFSubGraph;
import gilp.rdf3x.RDFSubGraphSet;
import gilp.rdf3x.Triple;
import gilp.rules.*;
import gilp.utils.KVPair;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;


public class PGEngine implements QueryEngine {
	
	public static final String CONSISTENT_TABLE = "consistent";
	public static final String INCONSISTENT_TABLE = "inconsistent";

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
		// TODO Auto-generated method stub
		return null;
	}
	
	private class JoinedPredicate{
		String pred_name;
		String join_pos; // s or o
		
		JoinedPredicate(String pred, String pos){
			this.pred_name = pred;
			this.join_pos = pos;
		}
	}
	
	//we assume the RDF data are stored in a r-db where each table stores triples 
	//with the same predicate.
	//TODO: currently we do not support queries where variables appear in predicates
	//TODO: we also do not consider self-join 
	private String buildSQL(Clause cls){
		
		StringBuffer sb_sel = new StringBuffer("select ");
		StringBuffer sb_from = new StringBuffer(" from ");
		StringBuffer sb_where = new StringBuffer(" where ");
		
		HashMap<String, Integer> hmap_preds = new HashMap<>(); 
		//ArrayList<String> listPredicates = new ArrayList<String>(); 
		HashMap<String,ArrayList<JoinedPredicate>> hmapJoins = new HashMap<String,ArrayList<JoinedPredicate>>();

		ArrayList<RDFPredicate> tpIterator = cls.getIterator();
		boolean hasWhereClause = false;
		for (RDFPredicate tp : tpIterator) {
			String prop_name = tp.getPredicateName();

			if (!hmap_preds.containsKey(tp.getPredicateName())) {
				hmap_preds.put(prop_name, 1);
				prop_name += "_1";
			} else {
				int num = hmap_preds.get(prop_name);
				num += 1;
				hmap_preds.put(prop_name, num);
				prop_name += ("_" + num);
			}

			sb_from.append(tp.getPredicateName() + " as ").append(prop_name).append(" , ");

			//if (!listPredicates.contains(tp.getPredicateName())){
			//	listPredicates.add(tp.getPredicateName());
			//}
			sb_sel.append(prop_name + ".tid, " + prop_name + ".S, ");
			if (tp.isSubjectVariable()) {
				ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(tp.getSubject());
				if (joinedPredicates == null) {
					joinedPredicates = new ArrayList<>();
					hmapJoins.put(tp.getSubject(), joinedPredicates);
				}
				joinedPredicates.add(new JoinedPredicate(prop_name, "s"));
			} else {
				//s is a constant
				sb_where.append(prop_name + ".s='" + tp.getSubject() + "'");
				sb_where.append(" and ");
				hasWhereClause = true;
			}

			if (tp.isObjectVariable()) {
				ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(tp.getObject());
				if (joinedPredicates == null) {
					joinedPredicates = new ArrayList<>();
					hmapJoins.put(tp.getObject(), joinedPredicates);
				}
				joinedPredicates.add(new JoinedPredicate(prop_name, "o"));
				if (tp.isObjectNumeric())
					sb_sel.append(prop_name + ".NUM_O, ");
				else
					sb_sel.append(prop_name + ".O, ");
			} else {
				//o is a constant
				hasWhereClause = true;
				if (tp.isObjectNumeric()) {
					double[] bounds = tp.getObjBounds();
					sb_where.append(prop_name + ".num_o between " + bounds[0] + " and " + bounds[1] + " ");
					sb_sel.append(prop_name + ".NUM_O, ");
				} else {
					sb_where.append(prop_name + ".o='" + tp.getObject() + "'");
					sb_sel.append(prop_name + ".O, ");
				}
				sb_where.append(" and ");
			}
		}
 		
		for (String var: hmapJoins.keySet()){
			ArrayList<JoinedPredicate> joinedPredicates = hmapJoins.get(var);
			if (joinedPredicates.size() < 2) 
				continue;//not a join
			else{
				for (int i=0;i<joinedPredicates.size()-1;i++){
					JoinedPredicate pr1 = joinedPredicates.get(i);
					JoinedPredicate pr2 = joinedPredicates.get(i+1);
					sb_where.append(pr1.pred_name + "." + pr1.join_pos);
					sb_where.append("=");
					sb_where.append(pr2.pred_name + "." + pr2.join_pos);
					sb_where.append(" and ");
				}
			}
		}
		
		String sel = sb_sel.toString();
		sel = sel.substring(0, sel.lastIndexOf(","));
		
		String from = sb_from.toString();
		from = from.substring(0, from.lastIndexOf(","));
		String where = sb_where.toString();
		if (where.indexOf("and")>=0)
			where = where.substring(0, where.lastIndexOf("and"));

		if (!hasWhereClause)
			where = "";
	
 		return sel + from + where;
	}
	
	 
	//Find the P_Hats (true positives) and and NHats (false positive) for a rule which is obtained by expanding @ro with @tp
	//One argument  of @tp must be shared with @r0, and the other argument, say X, of @tp is a fresh variable.  
	//This function will retrieve from KB all constants appearing in X and join them with @r0
	//The result will be saved in @listPHats and @hmapNHats
	//@listPHats contains pairs of <v_i, n_i>, where v_i is a constant, and n_i is the corresponding P_Hat
	//if v_i is used as an argument of  @tp  to expand @r0.
	//Elements in @listPHats are in descending order of n_i.
	//@hmapNHats contains <v_i, nhat_i>
	public boolean getPHatNhats(RulePackage rp, RDFPredicate tp, ArrayList<KVPair<String, Integer>> listPHats, HashMap<String,Integer> hmapNHats){
		//TODO
		/*Example
		 * r0 :   hasGivenName(X, Y) AND wasLivenIn(X, Shanghai) --> incorrect_hasGivenName(X, Y)
		 * tp:  rdftype(X, Z) 
		 * 1, we build an empty table: temp_triples_by_rule 
		 * temp_triples_by_rule ( hasGivenName_1_S, hasGivenName_1_O, wasLiveIn_1_S, wasLiveIn_1_O, incorrect_hasGivenName_S, incorrect_hasGivenName_O) 
		 * 2. get the instantiations covered by r0 in fb and insert them into table temp_triples_by_rule
		 * 3. execute the following SQL 
		 * select rdftype.O, count( distinct incorrect_hasGivenName_S || '-' || incorrect_hasGivenName_O) as PHat, 
		 * count( distinct hasGivenName_1_S || '-' || hasGivenName_1_O) as COV  
		 * from temp_triples_by_rule , rdftype 
		 * where temp_triples_by_rule.hasGivenName_1_S=rdftype.S 
		 * group by rdftype.O 
		 * order by PHat desc, COV
 
 		 * NOTE: the argument shared by @r0 and @tp may be a constant, e.g. 
		 * r0: wasLivenIn(X, Shanghai) --> incorrect_hasGivenName(X, Shanghai)
		 * tp: rdftype(Shanghai, Y)
		 * for this case, we need to add a where clause, e.g.  rdftype.S = 'Shanghai' 
		 * 4. build the result-list based on the returned tuples 
		 * */
		Feedback fb = rp.getFeedback();
		RDFRuleImpl r0 = rp.getRule();
		
		String temp_table = "temp_triples_by_rule";
		//1. build the temporary table 
		String sql = "DROP Table IF EXISTS " + temp_table;
		if (!DBController.exec_update(sql)){
			return false;
		}
		
		sql = "create table " + temp_table + "(";
		HashMap<String, Integer> hmap_preds = new HashMap<>();
		ArrayList<RDFPredicate> iterPreds = r0.get_body().getIterator();
		boolean findJoinedArgument = false;
		String joinedArgument = ""; 
		String joinedPositionInTP = ""; 
		String joinedPropInRule = ""; 
		
		ArrayList<String> head_vars = ((RDFPredicate)r0.get_head()).getVariables(); 
		HashMap<String,String> hmap_headvar_attr =new HashMap<> ();
		for (RDFPredicate p : iterPreds) {
			String prop_name = p.getPredicateName();
			if (!hmap_preds.containsKey(p.getPredicateName())) {
				hmap_preds.put(prop_name, 1);
				prop_name += "_1";
			} else {
				int num = hmap_preds.get(prop_name);
				num += 1;
				hmap_preds.put(prop_name, num);
				prop_name += ("_" + num);
			}
			sql += prop_name + "_S character varying(1024),";
			sql += prop_name + "_O character varying(1024),";

			if (p.isSubjectVariable()) {
				for (String v : head_vars) {
					if (p.getSubject().equals(v)) {
						hmap_headvar_attr.put(v, prop_name + "_S");
					}
				}
			}
			if (p.isObjectVariable()) {
				for (String v : head_vars) {
					if (p.getObject().equals(v)) {
						hmap_headvar_attr.put(v, prop_name + "_O");
					}
				}
			}

			//find the joined argument
			if (!findJoinedArgument) {
				JoinType[] jt = RDFPredicate.getJoinTypes(p, tp);
				if (jt != null) {
					findJoinedArgument = true;
					switch (jt[0]) {
						case SS:
							joinedPropInRule = prop_name + "_S";
							joinedArgument = tp.getSubject();
							joinedPositionInTP = "S";
							break;
						case SO:
							joinedPropInRule = prop_name + "_S";
							joinedArgument = tp.getObject();
							joinedPositionInTP = "O";
							break;
						case OS:
							joinedPropInRule = prop_name + "_O";
							joinedArgument = tp.getSubject();
							joinedPositionInTP = "S";
							break;
						case OO:
							joinedPropInRule = prop_name + "_O";
							joinedArgument = tp.getObject();
							joinedPositionInTP = "O";
							break;
					}
				}
			}
		}
		
		//check the selectivity table first
		// JoinSelect(condition1, condition2, selc, maxMatched)
		String query = "select maxMatched from JoinSelect where ";
		String cond1 = new String(joinedPropInRule); 
		cond1 = cond1.substring(0, cond1.indexOf("_")); 
		cond1 += joinedPropInRule.substring(joinedPropInRule.length()-2);
		String cond2 = tp.getPredicateName() + "_" + joinedPositionInTP; 
		if (cond1.compareToIgnoreCase(cond2)>0){
			String temp = cond1;
			cond1 = cond2;
			cond2 = temp;
		}
		query += " condition1='" + cond1 + "' and condition2='" + cond2 + "'"; 
		String rlt = DBController.getSingleValue(query);
		if (rlt == null){
			return true;
		}
		else{
			int maxMatched = Integer.parseInt(rlt);
			if (maxMatched<= GILPSettings.MINIMUM_MAX_MATCHED){
				return true;
			}
		}
		
 			
		if(!findJoinedArgument){
			//GILPSettings.log(this.getClass().getName() + " there are no common arguments between the original rule and the predicate.");
			return false;
		}
		
		sql += r0.get_head().getPredicateName() + "_S character varying(1024),";
		sql += r0.get_head().getPredicateName() + "_O character varying(1024)";
		
		//sql = sql.substring(0, sql.lastIndexOf(","));
		sql += ")"; 
		
		System.out.println(sql);
		
		if (!DBController.exec_update(sql)){
			//GILPSettings.log(this.getClass().getName() + " there is error when creating table consistent.");
			return false;
		}
	
		//2. get the triples covered by r0 in fb ( Body(F) left join H(T) ) and insert them into table consistent
		RDFSubGraphSet sg_set = rp.getSubgraphsCoveredByRule();
		for(RDFSubGraph sg: sg_set.getSubGraphs()){
			sql = "insert into " + temp_table + " values("; 
			for (Triple t: sg.getTriples()){
				if (t == null)
					sql += "null, null,";
				else
					sql += "'" + t.get_subject() + "', '" + t.get_obj() + "', "; 
			}
			sql = sql.substring(0, sql.lastIndexOf(","));
			sql += ")"; 
			
			System.out.println(sql);
			
			if (!DBController.exec_update(sql)){
			//	GILPSettings.log(this.getClass().getName() + " there is error when inserting tuples into table consistent.");
				return false;
			}	
		}	
		
		// 3. execute the aggreation SQL 
		String aggregate_att = tp.getPredicateName() + "."; 		
		if (joinedPositionInTP=="S"){
			if (tp.isObjectNumeric())
				aggregate_att += "num_o";
			else
				aggregate_att += "O";
		}
		else
			aggregate_att += "S"; 
		
		String str_sel = "select count( distinct "; 
		RDFPredicate head = (RDFPredicate)r0.get_head();
		if (head.isSubjectVariable())
			str_sel += head.getPredicateName() + "_S";
		if (head.isObjectVariable())
			str_sel += " || '-' || " + head.getPredicateName() + "_O"; 
		
		str_sel += ") as PHat, count( distinct ";  
		
		for (int i=0;i<head_vars.size();i++){
			String v = head_vars.get(i);
			if (i>0)
				str_sel += " || '-' || "; 
			String attr = hmap_headvar_attr.get(v);
			str_sel += attr; 
		}
		str_sel += ") as COV ";
		
		String str_from = " from " + temp_table +" , " + tp.getPredicateName();
		String str_where = " where " + temp_table + "." + joinedPropInRule + "=" + tp.getPredicateName() + "." + joinedPositionInTP;
		
		if (joinedPositionInTP=="S" && !tp.isSubjectVariable()){
			str_where += " and " + tp.getPredicateName() + ".S='" + joinedArgument + "'";
		}
		if (joinedPositionInTP=="O" && !tp.isObjectVariable()){
			str_where += " and " + tp.getPredicateName() + ".O='" + joinedArgument + "'";
		}
		
		String str_group = " group by " + aggregate_att; 
		String str_order = " order by PHat desc, COV" ; 
		
		//compute p_hats and n_hats for variable atom
		sql = str_sel + str_from + str_where; 
		//System.out.println(sql);
		ArrayList<ArrayList<String>> listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		ArrayList<String> tuple = listTuples.get(0);
		int p_hat = Integer.parseInt(tuple.get(0));
		int n_hat = Integer.parseInt(tuple.get(1)) - p_hat;
		listPHats.add(new KVPair<String, Integer>("--variable--", p_hat)); 
		hmapNHats.put("--variable--", n_hat);
		
		//compute p_hats and n_hats for constant atoms
		str_sel = str_sel.replaceFirst("select", "select " + aggregate_att + " , "); 
		sql = str_sel + str_from + str_where + str_group + str_order;
		//System.out.println(sql);
		
		listTuples = DBController.getTuples(sql);
		if (listTuples == null)
			return true;
		
		//constant, PHat, COV
		for(ArrayList<String> tuple1: listTuples){
			String val = tuple1.get(0);
			p_hat = Integer.parseInt(tuple1.get(1));
			n_hat = Integer.parseInt(tuple1.get(2)) - p_hat;
			listPHats.add(new KVPair<String, Integer>(val, p_hat)); 
			hmapNHats.put(val, n_hat);
		}
		return true;
	}
	
	boolean containRDFType(Clause cls){
		ArrayList<RDFPredicate> iter = cls.getIterator();
		for (RDFPredicate rdfPredicate : iter) {
			if (rdfPredicate.getPredicateName().equalsIgnoreCase("rdftype")) {
				return true;
			}
		}
		return false;
	}
	
	Clause replaceRDFType(Clause cls, String newPred){
		Clause newCls = cls.clone();
		ArrayList<RDFPredicate> iter = newCls.getIterator();
		for (RDFPredicate p : iter) {
			if (p.getPredicateName().equalsIgnoreCase("rdftype")) {
				p.setPredicateName(newPred);
			}
		}
		return newCls;
	}
	
	public RDFSubGraphSet getTriplesByCNF(Clause cls, int num){
		if(containRDFType(cls)){
			RDFSubGraphSet rltSet = new RDFSubGraphSet();
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(cls, newPred);
				String sql = this.buildSQL(convertedCls);
				sql += "  limit " + num; 
				RDFSubGraphSet oneSet = doQuery(cls, sql);
				if (i==0)
					rltSet.setPredicates(oneSet.getPredicates());
				for (RDFSubGraph sg: oneSet.getSubGraphs())
					rltSet.addSubGraph(sg);
			}
			//do sampling
			if(rltSet.getSubGraphs().size()>num){
				int s = rltSet.getSubGraphs().size();
				int[] isChosen = new int[s];
				for (int i = 0; i < s; i++) {
					isChosen[i] = 0;
				}
				ArrayList<RDFSubGraph> rltSetCopy = new ArrayList<>(rltSet.getSubGraphs());

				ArrayList<RDFSubGraph> listChosenSGs = new ArrayList<>();
				int i=0;
				while (listChosenSGs.size() < Math.min(num, s)) {
					int idx = (int) Math.round(Math.random() * (s - 1));
					if (isChosen[idx] == 0) {
					//	RDFSubGraph sg = rltSet.getSubGraphs().get(idx);
						RDFSubGraph sg = rltSetCopy.get(idx);
						isChosen[idx] = 1;
						listChosenSGs.add(sg);
					}
					i++;
				}
				rltSet.getSubGraphs().clear();
				rltSet.getSubGraphs().addAll(listChosenSGs);
			}
			return rltSet;
		}
		else{		
			String sql = this.buildSQL(cls);
			sql += "  limit " + num; 
			return doQuery(cls, sql);
		}
	}

	@Override
	public RDFSubGraphSet getTriplesByCNF(Clause cls) {
		if(containRDFType(cls)){
			RDFSubGraphSet rltSet = new RDFSubGraphSet();
			for(int i=0;i<GILPSettings.NUM_RDFTYPE_PARTITIONS;i++){
				String newPred = "rdftype" + i; 
				Clause convertedCls = replaceRDFType(cls, newPred);
				String sql = this.buildSQL(convertedCls);
System.out.println(sql);
				RDFSubGraphSet oneSet = doQuery(cls, sql);
				if (i==0)
					rltSet.setPredicates(oneSet.getPredicates());
				for (RDFSubGraph sg: oneSet.getSubGraphs())
					rltSet.addSubGraph(sg);
			}			
			return rltSet;
		}
		else{
			String sql = this.buildSQL(cls);
			return doQuery(cls, sql);
		}
	}
		
	// get at most @num sub-graphs
	private RDFSubGraphSet doQuery(Clause cls, String query) {
		// execute the SPARQL

		ResultSet rs = null;				 
		RDFSubGraphSet sg_set = null;
		try
		{
		//	conn = DBController.getConn();
		//	pstmt = conn.prepareStatement(query);
			rs = new DBController().execQuery(query);
			sg_set =  mountSGSet(rs, cls);
				
		}catch (Exception e)
		{
			//GILPSettings.log(this.getClass().getName() + "Error! ");
			e.printStackTrace();
			return null;
		}finally
		{
			DBController.close();
			//DBPool.closeAll(conn, pstmt, rs);
		}	
		return sg_set;
	}

	
	// This function will mount sug-graphs based on the tuples returned by PG and clause (pattern of the sub-graphs).
	// The columns returned by PG are ordered in the same way as they appear in the cls. 
	// Every 3 columns (tid, s, o) correspond to a predicate . 	
	private RDFSubGraphSet mountSGSet(ResultSet rlt, Clause cls) {

		ArrayList<RDFPredicate> myIter = cls.getIterator();
		HashSet<RDFPredicate> preds = new HashSet<>(myIter);
		// initialize the graph set
		RDFSubGraphSet sg_set = new RDFSubGraphSet();
		sg_set.setPredicates(preds);

		// for each result tuple, we mount a sub-graph
		try {

			while (rlt.next()) {
				RDFSubGraph sg = new RDFSubGraph();
				int i=0;
				for(RDFPredicate tp :preds){

			//	for (int i = 0; i < preds.size(); i++) {
				//	RDFPredicate tp = preds.get(i);
					Triple t = new Triple();
					t.set_predicate(tp.getPredicateName());
					t.set_subject(rlt.getString(i*3+2));
					t.set_obj(rlt.getString(i*3+3));
					sg.addTriple(t);
					i++;
				}
				sg_set.addSubGraph(sg);
			}
		} catch (SQLException e) {
			e.printStackTrace(System.out);
			return null;
		}

		return sg_set;
	}
	
	//###############################################################################

	//                   unit  tests
	
	//###############################################################################
	static void testSimpleQuery(){
		Clause cls = new Clause();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("hasGivenName");
		tp.setObject(new String("?o"));
		cls.addPredicate(tp);
	 
		tp = new RDFPredicate();
		tp.setSubject(new String("?s"));
		tp.setPredicateName("rdftype");
		tp.setObject(new String("wikicat_Chinese_people"));		
		cls.addPredicate(tp);
		
		PGEngine pg = new PGEngine();
		System.out.println(pg.buildSQL(cls));
		
		
		System.out.println("#######################################################");
		try {
			System.out.println("query is:" + cls.toString());
			RDFSubGraphSet rlt = pg.getTriplesByCNF(cls);
			System.out.println("There are " + rlt.getSubGraphs().size() + " results:");
			if (rlt!=null){
				ArrayList<Triple> triples = rlt.getTriplesByPredicate("hasGivenName");
				RandomAccessFile file = new RandomAccessFile("/home/jchen/gilp/chinese_persons.txt","rw");
				
				for (Triple t: triples){
					String str = t.toString();
					str = str.replaceAll("<", "");
					str = str.replaceAll(">", "");
					file.writeBytes(str + " -1\n");
					System.out.println(t);
				}
				//for (RDFSubGraph twig: rlt.getSubGraphs()){
				//	System.out.println(twig.toString());
				//}
				file.close();
			}
			else{
				System.out.println("Empty results set.");
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace(System.out);
		}
		
	}
	
	static void testPNHats(){
		
		ArrayList<Comment> listComments = new ArrayList<Comment>();

		Triple t;
		Comment cmt;
		RandomAccessFile file_data = null;

		try {
			file_data = new RandomAccessFile("./knowledgeClean-data/yago-data clean/comments.txt", "r");
			String line = "";
			while ((line = file_data.readLine()) != null) {
				StringTokenizer st = new StringTokenizer(line, " ");
				String s, p, o;
				s = st.nextToken();
				p = st.nextToken();
				o = st.nextToken();
				int d = Integer.parseInt(st.nextToken());
				t = new Triple(s, p, o);
				cmt = new Comment(t, (d > 0));
				listComments.add(cmt);
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		Feedback fb = new Feedback();
		fb.set_comments(listComments);
		
		Clause cls = new Clause();
		RDFPredicate tp = new RDFPredicate();
		tp.setSubject("?s1");
		tp.setPredicateName("hasGivenName");
		tp.setObject( "?o1");
		cls.addPredicate(tp);
		
		RDFPredicate tp1 = tp.mapToIncorrectPred(); 		
	 
		RDFRuleImpl r0 = new RDFRuleImpl(); 
		r0.set_body(cls);
		r0.set_head(tp1);
		
		PGEngine pg = new PGEngine();
		
		RDFPredicate ex_tp = new RDFPredicate(); 
		ex_tp.setPredicateName("hasFamilyName");
		ex_tp.setSubject("?s1");
		ex_tp.setObject("?o2");
		
		ArrayList<KVPair<String, Integer>> listPHats = new ArrayList<>(); 
		HashMap<String, Integer> hmapNHats = new HashMap<>();
		
		RulePackage rp = new RulePackage(r0, fb, null); 		
		pg.getPHatNhats(rp, ex_tp, listPHats, hmapNHats);

		System.out.println("test");
		for (KVPair<String, Integer> kv: listPHats){
			String val = kv.getKey();
			int p_hat = kv.getValue();
			int n_hat = hmapNHats.get(val);
			System.out.println(val + "|" + p_hat + "|" + n_hat);
		}
	}
	
	public static void main(String[] args){
		//testPNHats();
		testSimpleQuery();
	}

}
