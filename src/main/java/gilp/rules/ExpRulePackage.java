package gilp.rules;

import gilp.comments.Feedback;

//an expanded rule. 
//CJC DEC 31, 2016
public class ExpRulePackage extends RulePackage {
	
	//For expanded rules, we do not need to store their feedbacks, the pHat and nHat will be given.	 
	public ExpRulePackage(RDFRuleImpl r, RulePackage baseRP, double pHat, double nHat){
		 super(r, null, baseRP);//set feedback as null
		 this._PHat = pHat;
		 this._NHat = nHat;
		 init();
	} 
	
	void init(){
		this._quality = -1; 	
		this._kb_support = -1; 
		this._fb_support = -1;
		this._precision = -1;
	}
		
	@Override 
	public double getPHat() {
		return _PHat;
	} 
	@Override
	public double getNHat() {
		return _NHat;
	}
	
	public ExpRulePackage clone(){
		ExpRulePackage new_rp = null;
		if (this._base_RP!=null)
			new_rp = new ExpRulePackage(this._rule.clone(),this._base_RP.clone(), this._PHat, this._NHat);
		else
			new_rp = new ExpRulePackage(this._rule.clone(),null, this._PHat, this._NHat);
		
		return new_rp;
	}
	
	@Override
	@Deprecated
	public double getFBSupport(){
		//GILPSettings.log(this.getClass().getName() + "Error! The getFBSupport is not supported by ExpRulePackage.");
		return -1;
	}
	
	@Override
	@Deprecated
	void calcSupportInFB(){
		//GILPSettings.log(this.getClass().getName() + "Error! The calcSupportInFB is not supported by ExpRulePackage.");
	}
	
	@Override 
	@Deprecated
	public void setBaseRP(RulePackage rp) {
	//	GILPSettings.log(this.getClass().getName() + "Error! The setBaseRP is not supported by ExpRulePackage.");
	}
 
	@Override
	@Deprecated
	public void setRule(RDFRuleImpl r){
	//	GILPSettings.log(this.getClass().getName() + "Error! The setRule is not supported by ExpRulePackage.");
	} 
	
	@Override
	@Deprecated
	public void setFeedback(Feedback fb){
	//	GILPSettings.log(this.getClass().getName() + "Error! The setFeedback is not supported by ExpRulePackage.");
	} 
	@Override
	@Deprecated
	public Feedback getFeedback(){
	//	GILPSettings.log(this.getClass().getName() + "Error! The getFeedback is not supported by ExpRulePackage.");
		return null; 
	}
	
	@Override
	@Deprecated 
	void calcPN_Hats() {
	//	GILPSettings.log(this.getClass().getName() + "Error! The calcPN_Hats is not supported by ExpRulePackage.");
	} 
}
