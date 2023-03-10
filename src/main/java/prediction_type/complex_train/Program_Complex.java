package prediction_type.complex_train;

public class Program_Complex {
	public static void main(String[] args) throws Exception {
		Arguments cmmdArg = new Arguments(args);
		ComplexModel complex = new ComplexModel();
		String fnTrainingTriples = "";
		String fnValidateTriples = "";
		String fnTestingTriples = "";
		String strNumRelation = "";
		String strNumEntity = "";
		
		try {
			fnTrainingTriples = cmmdArg.getValue("train");
			if (fnTrainingTriples == null || fnTrainingTriples.equals("")) {
				Usage();
				return;
			}
			fnValidateTriples = cmmdArg.getValue("valid");
			if (fnValidateTriples == null || fnValidateTriples.equals("")) {
				Usage();
				return;
			}
			fnTestingTriples = cmmdArg.getValue("test");
			if (fnTestingTriples == null || fnTestingTriples.equals("")) {
				Usage();
				return;
			}
			strNumRelation = cmmdArg.getValue("m");
			if (strNumRelation == null || strNumRelation.equals("")) {
				Usage();
				return;
			}
			strNumEntity = cmmdArg.getValue("n");
			if (strNumEntity == null || strNumEntity.equals("")) {
				Usage();
				return;
			}
			if (cmmdArg.getValue("k") != null && !cmmdArg.getValue("k").equals("")) {
				complex.NumFactor = Integer.parseInt(cmmdArg.getValue("k"));
			}
			if (cmmdArg.getValue("d") != null && !cmmdArg.getValue("d").equals("")) {
				complex.Theta = Double.parseDouble(cmmdArg.getValue("d"));
			}
			if (cmmdArg.getValue("ne") != null && !cmmdArg.getValue("ne").equals("")) {
				complex.NumNeg = Integer.parseInt(cmmdArg.getValue("ne"));
			}
			if (cmmdArg.getValue("ge") != null && !cmmdArg.getValue("ge").equals("")) {
				complex.GammaE = Double.parseDouble(cmmdArg.getValue("ge"));
			}
			if (cmmdArg.getValue("gr") != null && !cmmdArg.getValue("gr").equals("")) {
				complex.GammaR = Double.parseDouble(cmmdArg.getValue("gr"));
			}
			if (cmmdArg.getValue("#") != null && !cmmdArg.getValue("#").equals("")) {
				complex.NumIteration = Integer.parseInt(cmmdArg.getValue("#"));
			}
			if (cmmdArg.getValue("skip") != null && !cmmdArg.getValue("skip").equals("")) {
				complex.OutputIterSkip = Integer.parseInt(cmmdArg.getValue("skip"));
			}
			long startTime = System.currentTimeMillis();
			complex.Initialization(strNumRelation, strNumEntity, fnTrainingTriples, fnValidateTriples, fnTestingTriples);
			
			System.out.println("\nStart learning ComplEx model (triples only)!");
			complex.Complex_Learn();
			System.out.println("Success.");
			long endTime = System.currentTimeMillis();
			System.out.println("All running time:" + (endTime-startTime)+"ms");
		} catch (Exception e) {
			e.printStackTrace();
			Usage();
			return;
		}
	}
	
	static void Usage() {
		System.out.println(
				"Usagelala: java -jar ComplEx.jar -train training_triple_path -valid validate_triple_path -test test_triple_path -m number_of_relations -n number_of_entities [options]\n\n"
				+
				"Options: \n"
				+ "   -k        -> number of latent factors (default 20)\n"
				+ "   -d        -> threshold delta (default 1.0)\n"
				+ "   -ne       -> number of negatives per positive triple (default 2)\n"
				+ "   -ge       -> learning rate of matrix E (default 0.01)\n"
				+ "   -gr       -> learning rate of tensor R (default 0.01)\n"
				+ "   -#        -> number of iterations (default 1000)\n"
				+ "   -skip     -> number of skipped iterations (default 50)\n\n"
				);
	}
}
