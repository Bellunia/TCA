package MLNs.inference;

import MLNs.util.Bundle;
import com.googlecode.rockit.app.solver.pojo.Clause;
import com.googlecode.rockit.app.solver.pojo.Literal;
import com.googlecode.rockit.javaAPI.HerbrandUniverse;
import MLNs.Controller.NameMapper.Type;
import MLNs.Controller.ProbKBData;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;

/**
 * The "factors" singleton makes the three collections needed by the RockIt
 * inference out of ProbKB output.
 * 
 * @author Tommaso Soru <tsoru@informatik.uni-leipzig.de>
 *
 */
public class Factors {

	private final static Logger logger = LogManager.getLogger(Factors.class);
	
	private static Factors instance = null;

	private ArrayList<String> consistentStartingPoints;
	private ArrayList<Clause> clauses;
	private Collection<Literal> evidence;

	private PostgreDB db;

	private static HerbrandUniverse u = HerbrandUniverse.getInstance();

	protected Factors() {
		super();
	}

	public static Factors getInstance() {
		if (instance == null)
			instance = new Factors();
		return instance;
	}

	/**
	 * Preprocess factors from ProbKB for RockIt.
	 * 
	 * @param aimName
	 */
	public void preprocess(String aimName) {

		db = new PostgreDB();
		db.connect();
		System.out.println("test-connection");

		buildClauses();
		buildEvidence(aimName);

		db.close();
	}

	private void buildEvidence(String aimName) {
		
		evidence = new ArrayList<>();
		consistentStartingPoints = new ArrayList<>();

		ResultSet rs;
		if(aimName == null) {
			rs = db.evidence();
		} else {
			int aimNumber = Integer.parseInt(aimName
				.substring(ProbKBData.REL_LENGTH));
			rs = db.evidence(aimNumber);
		}
		
		try {
			while (rs.next()) {
				String a1 = u.getKey(Type.ENTITY.name() + rs.getInt("ent1"));
				String b1 = u.getKey(Type.ENTITY.name() + rs.getInt("ent2"));
				String r = Type.RELATION.name() + rs.getInt("rel");
//				String string = aimName + "|" + a1 + "|" + b1;
				String string = r + "|" + a1 + "|" + b1;
				// As the Semantic Web deals only with true statements,
				// all literals are set to true and belong to the starting
				// points.
				consistentStartingPoints.add(string);
				evidence.add(new Literal(string, true));
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}

		logger.info("EVIDENCE SIZE = "+evidence.size());
//		logger.debug("EVIDENCE");
//		for (Literal l : evidence)
//			logger.debug(l);

	}

	private void buildClauses() {

		clauses = new ArrayList<>();

		for (int i = 1; i <= 3; i++) {
			
			logger.debug("Selecting type "+i+" factors...");
			ResultSet rs = db.factors(i);
			try {
				while (rs.next()) {

					ArrayList<Literal> lit = new ArrayList<>();
					boolean positive = true;

					// first restriction
					String r1 = Type.RELATION.name() + rs.getInt("r1");
					String a1 = u.getKey(Type.ENTITY.name() + rs.getInt("a1"));
					String b1 = u.getKey(Type.ENTITY.name() + rs.getInt("b1"));
					lit.add(new Literal(r1 + "|" + a1 + "|" + b1, positive));

					if (i >= 2) {
						// second restriction
						String r2 = Type.RELATION.name() + rs.getInt("r2");
						String a2 = u.getKey(Type.ENTITY.name() + rs.getInt("a2"));
						String b2 = u.getKey(Type.ENTITY.name() + rs.getInt("b2"));
						lit.add(new Literal(r2 + "|" + a2 + "|" + b2, positive));

						if (i >= 3) {
							// third restriction
							String r3 = Type.RELATION.name() + rs.getInt("r3");
							String a3 = u.getKey(Type.ENTITY.name() + rs.getInt("a3"));
							String b3 = u.getKey(Type.ENTITY.name() + rs.getInt("b3"));
							lit.add(new Literal(r3 + "|" + a3 + "|" + b3,
									positive));
						}
					}

					// XXX Since there is a weight, its value is finite
					// (hard=false).
					boolean hard = false;
					
					Clause clause = new Clause(rs.getDouble("weight"), lit, hard);
					clauses.add(clause);
					logger.trace(clause);

				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			logger.debug(clauses.size() + " clauses collected until type "+i+".");
		}

		logger.info(clauses.size() + " clauses collected.");

	}

	public ArrayList<String> getConsistentStartingPoints() {
		return consistentStartingPoints;
	}

	public ArrayList<Clause> getClauses() {
		return clauses;
	}

	public Collection<Literal> getEvidence() {
		return evidence;
	}

}
