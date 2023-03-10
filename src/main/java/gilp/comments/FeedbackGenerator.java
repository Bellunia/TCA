package gilp.comments;

import gilp.query.PGEngine;

import gilp.rdf3x.RDFSubGraphSet;
import gilp.rdf3x.Triple;
import gilp.rules.Clause;
import gilp.rules.RDFPredicate;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * 
 * */
public class FeedbackGenerator {
	private ArrayList<Comment> _known_comments;	
		
	//construct a set of comments which will be used to generate feedbacks
	void initialize(){
		this._known_comments = new ArrayList<Comment>();
		/*current solution is simple: we predefined several rules, and regard the corresponding triples are incorrect*/
		ArrayList<Triple> listTriples = null;
		Clause cls_UnEmployment = new Clause();
		
		cls_UnEmployment.addPredicate(new RDFPredicate("?s", "hasUnEmployment", "[100, 1000000000]"));
		listTriples = retrieveTriples(cls_UnEmployment, "hasUnEmployment");
		this._known_comments.addAll(buildComments(listTriples, false));
		
		
		Clause cls_chinese = new Clause();
		cls_chinese.addPredicate(new RDFPredicate("?s", "hasGivenName", "?o"));
		cls_chinese.addPredicate(new RDFPredicate("?s", "isCitizenOf", "China"));
		
		listTriples = retrieveTriples(cls_chinese, "hasGivenName");
		this._known_comments.addAll(buildComments(listTriples, false)); 
		
		Clause cls_us = new Clause();
		cls_us.addPredicate(new RDFPredicate("?s", "hasGivenName", "?o"));
		cls_us.addPredicate(new RDFPredicate("?s", "isCitizenOf", "Hungary"));
		
		listTriples = retrieveTriples(cls_us, "hasGivenName");
		this._known_comments.addAll(buildComments(listTriples, true)); 

		Clause cls_sh = new Clause();
		cls_sh.addPredicate(new RDFPredicate("?s", "hasGivenName", "?o"));
		cls_sh.addPredicate(new RDFPredicate("?s", "wasBornIn", "Shanghai"));
		
		listTriples = retrieveTriples(cls_sh, "wasBornIn");
		this._known_comments.addAll(buildComments(listTriples, true));
	}
	
	private ArrayList<Comment> buildComments(ArrayList<Triple> listTriples, boolean decision){
		ArrayList<Comment> listRlts = new ArrayList<Comment>();
		for (Triple t: listTriples){
			listRlts.add(new Comment(t.clone(), decision));
		}
		return listRlts;
	}
	
	private ArrayList<Triple> retrieveTriples(Clause cls, String predicate){
		//RDF3XEngine qe = new RDF3XEngine(); 
		PGEngine qe = new PGEngine();
		RDFSubGraphSet sg_set = qe.getTriplesByCNF(cls);
		return sg_set.getTriplesByPredicate(predicate);
	}
	
	
	public FeedbackGenerator( ){
		initialize();
	}
	
	//randomly choose @num comments as a set of feedbacks
	public Feedback getRandomComments(int num){	 
		int s = this._known_comments.size();
		int[] isChosen = new int[s];
		for(int i=0;i<s;i++){
			isChosen[i] = 0;
		}
		
		ArrayList<Comment> listCmts = new ArrayList<Comment>();
		
		while(listCmts.size() < Math.min(num, s)){
			int idx = (int)Math.round(Math.random()*(s-1));
			if (isChosen[idx] == 0){
				listCmts.add(this._known_comments.get(idx).clone());
				isChosen[idx] = 1;
			}	
		}
		Feedback fb = new Feedback();
		fb.set_comments(listCmts);
		return fb;
	}
	
	//for each triple inside @listTriples, try to find its corresponding comment
	//from this._known_comments.
	public Feedback getComments(ArrayList<Triple> listTriples){
		HashMap<String,Comment> hmapComments = new HashMap<String,Comment>();
		for (Comment cmt: this._known_comments){
			hmapComments.put(cmt.get_triple().toString(), cmt);
		}
		
		ArrayList<Comment> listCmts = new ArrayList<Comment>();
		for (Triple t: listTriples){
			Comment cmt = hmapComments.get(t.toString());
			if (cmt != null){
				listCmts.add(cmt.clone());
			}
			else{
				cmt = new Comment(t,true);
				listCmts.add(cmt);
			}
		}
		Feedback fb = new Feedback();
		fb.set_comments(listCmts);
		return fb;
	}
}
