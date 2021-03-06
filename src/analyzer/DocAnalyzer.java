/**
 * 
 */
package analyzer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import json.JSONArray;
import json.JSONException;
import json.JSONObject;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;

import org.tartarus.snowball.SnowballStemmer;
import org.tartarus.snowball.ext.englishStemmer;
import org.tartarus.snowball.ext.porterStemmer;

import structures.Post;
import structures.Token;
/**
 * @author hongning
 * Sample codes for demonstrating OpenNLP package usage 
 * NOTE: the code here is only for demonstration purpose, 
 * please revise it accordingly to maximize your implementation's efficiency!
 */
public class DocAnalyzer {
	
	//a list of stopwords
	HashSet<String> m_stopwords;
	Set<String> m_vocab;
	
	//you can store the loaded reviews in this arraylist for further processing
	ArrayList<Post> m_reviews;
	ArrayList<Post> Query_reviews;
	
	Tokenizer tokenizer;
	
	//you might need something like this to store the counting statistics for validating Zipf's and computing IDF
	HashMap<String, Token> m_stats;
	Set<String> unigramTokenSet;
	Set<String> bigramTokenSet;
	int unigramSize;
	
	//we have also provided sample implementation of language model in src.structures.LanguageModel
	
	public DocAnalyzer() {
		m_reviews = new ArrayList<Post>();
		m_stats = new HashMap<String, Token>();
		m_stopwords = new HashSet<String>();
		Query_reviews = new ArrayList<Post>();
		unigramTokenSet = new HashSet<String>();
		bigramTokenSet = new HashSet<String>();
		unigramSize = 0;

		try{
			tokenizer = new TokenizerME(new TokenizerModel(new FileInputStream(
				"./data/Model/en-token.bin")));
		}
 catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	//sample code for loading a list of stopwords from file
	//you can manually modify the stopword file to include your newly selected words
	public void LoadStopwords(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			String line;

			while ((line = reader.readLine()) != null) {
				//it is very important that you perform the same processing operation to the loaded stopwords
				//otherwise it won't be matched in the text content
				line = SnowballStemmingDemo(NormalizationDemo(line));
				if (!line.isEmpty())
					m_stopwords.add(line);
			}
			reader.close();
			System.out.format("Loading %d stopwords from %s\n", m_stopwords.size(), filename);
		} catch(IOException e){
			System.err.format("[Error]Failed to open file %s!!", filename);
		}
	}

	// get the TTF of unigram and bigram,
	// store the bigram and unigram separately in a Set
	public void analyzeDocumentTTF(JSONObject json) {
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for (int i = 0; i < jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));

				String preToken = null;
				boolean flag = false;

				for (String token : tokenizer.tokenize(review.getContent())) {

					token = SnowballStemmingDemo(NormalizationDemo(token));
					if (token.isEmpty()) {
						continue;
					}

					Token tokenObject = null;
					if (!m_stats.containsKey(token)) {
						tokenObject = new Token(token);
						int curMapSize = m_stats.size();
						tokenObject.setID(curMapSize + 1);
						m_stats.put(token, tokenObject);
					} else {
						tokenObject = m_stats.get(token);
					}

					double curVal = tokenObject.getValue();
					tokenObject.setValue(curVal + 1);
					unigramTokenSet.add(token);
					unigramSize += 1;
					
					if (!flag) {
						preToken = token;
						flag = true;
						continue;
					}
					
					String biToken = preToken + "_" + token;
					Token biTokenObject = null;
					if (!m_stats.containsKey(biToken)) {
						biTokenObject = new Token(biToken);
						int curMapSize = m_stats.size();
						biTokenObject.setID(curMapSize + 1);
						m_stats.put(biToken, biTokenObject);
						
						curVal = biTokenObject.getValue();
						biTokenObject.setValue(curVal + 1);
					} else {
						biTokenObject = m_stats.get(biToken);
						curVal = biTokenObject.getValue();
						biTokenObject.setValue(curVal + 1);
					}

					bigramTokenSet.add(biToken);
					preToken = token;
				}

				m_reviews.add(review);
			}

			// System.out.println("bigramToken");
			// for(String bigramToken : bigramTokenSet){
			// System.out.println(bigramToken);
			// }
			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void getFollowingToken(String previousToken, int topK) {
		HashMap<String, Double> bigramModel = new HashMap<String, Double>();
		System.out.println("get Following Token");
		for (String token : unigramTokenSet) {

			String bigramToken = previousToken + "_" + token;// construct
																// "good"+"followingword"
			double bigramTokenProb = 0.0;
			double bigramTokenTTF = 0.0;
			double previousTokenTTF = 0.0;
			double tokenTTF = 0.0;
			double tokenProb = 0.0;
			

			double lambda = 0.9; //linear smoothing
			double delta = 0.1;// abosulute smoothing
			// if query is in bigram, then c(w_{i-1}w_{i}) is larger than 0
			if (bigramTokenSet.contains(bigramToken)) {
				bigramTokenTTF = m_stats.get(bigramToken).getValue(); // get TTF
			} else {//else is zero
				bigramTokenTTF = 0;
			}
			
			previousTokenTTF = m_stats.get(previousToken).getValue();

			tokenTTF = m_stats.get(token).getValue();
			tokenProb = tokenTTF / unigramSize; // unigram model prob

			//bigramTokenProb = linearSmoothing(lambda, bigramTokenTTF,
			// previousTokenTTF, tokenProb);

			bigramTokenProb = absoluteSmoothing(delta, bigramTokenTTF,
					previousTokenTTF, tokenProb, uniquePreBigram);
			bigramModel.put(token, bigramTokenProb);
			// System.out.println("tokenProb");
			// System.out.println(bigramTokenProb);
			// System.out.println(bigramToken);

		}

		System.out.println("topK");
		// bigramModel = sort(bigramModel);

		String fileName = "topK_" + previousToken + "word.txt";
		try {
			PrintStream out = new PrintStream(new FileOutputStream(fileName));
			for (String followingToken : bigramModel.keySet()) {

				out.println(followingToken + "\t"
						+ bigramModel.get(followingToken));
				// out.println(bigramModel.get(followingToken));
			}
			out.flush();
			out.close();
			System.out.println(m_stats.size());
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public double linearSmoothing(double lambda, double bigramTokenTTF,
			double previousTokenTTF, double tokenProb) {
		double bigramProb = 0.0;
		bigramProb = (1 - lambda) * (bigramTokenTTF / previousTokenTTF)
				+ lambda * tokenProb;
		
		return bigramProb;
	}
	
	public double absoluteSmoothing(double delta, double bigramTokenTTF,
			double previousTokenTTF, double unigramSize, double uniquePreBigram) {
		double term1 = 0.0;
		if (previousTokenTTF > 0.0) {
			term
		}
		double bigramProb = 0.0;
		bigramProb = (bigramTokenTTF + delta)
				/ (previousTokenTTF + delta * unigramSize);

		return bigramProb;
	}
	
	public HashMap<String, Double> sort(HashMap<String, Double> mapArg) {
		List mapKeys = new ArrayList(mapArg.keySet());
		List mapValues = new ArrayList(mapArg.values());

		Collections.sort(mapValues, Collections.reverseOrder());
		Collections.sort(mapKeys, Collections.reverseOrder());

		// Collections.sort(mapValues);
		// Collections.sort(mapKeys);

		HashMap<String, Double> duplicateMap = new HashMap<String, Double>();

		Iterator valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Object val = valueIt.next();
			Iterator KeyIt = mapKeys.iterator();

			while (KeyIt.hasNext()) {
				Object key = KeyIt.next();
				String comp1 = mapArg.get(key).toString();
				String comp2 = val.toString();

				if (comp1.equals(comp2)) {
					mapArg.remove(key);
					mapKeys.remove(key);
					duplicateMap.put((String) key, (Double) val);
					break;
				}

			}
		}

		return duplicateMap;

	}

	public void analyzeDocumentDF(JSONObject json) {
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for (int i = 0; i < jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));

				Token tokenObject = null;
				Set<String> tokenSet = new HashSet<String>();
				for (String token : tokenizer.tokenize(review.getContent())) {

					token = SnowballStemmingDemo(NormalizationDemo(token));

					if (token.isEmpty()) {
						continue;
					}

					if (!m_stats.containsKey(token)) {
						tokenObject = new Token(token);
						int curMapSize = m_stats.size();
						tokenObject.setID(curMapSize + 1);
						m_stats.put(token, tokenObject);
					} else {
						tokenObject = m_stats.get(token);
					}

					if (!tokenSet.contains(token)) {
						tokenSet.add(token);
						double curVal = tokenObject.getValue();
						tokenObject.setValue(curVal + 1);
					}

				}
				m_reviews.add(review);

				//
				// HINT: perform necessary text processing here, e.g.,
				// tokenization, stemming and normalization
				//
			}

		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void computeIDF() {
		for (String key : m_stats.keySet()) {
			double tokenDF = m_stats.get(key).getValue();
			double reviewSize = m_reviews.size();
			double tokenIDF = 1 + Math.log10(reviewSize / tokenDF);
			m_stats.get(key).setValue(tokenIDF);
		}
		System.out.println("computeIDF...");
	}
	
	//
	public void getDocumentWeight(ArrayList<Post> reviewsList) {
		for(Post review : reviewsList){

			String preToken = null;
			boolean flag = false;

			for (String token : tokenizer.tokenize(review.getContent())){
				
				Token tokenObject = null;
				token = SnowballStemmingDemo(NormalizationDemo(token));

				if (token.isEmpty()) {
					continue;
				}

				if (!m_vocab.contains(token)){
					continue;
				}

				review.addVectorValue(token);

				if (!flag) {
					preToken = token;
					flag = true;
					continue;
				}

				String biToken = preToken + "_" + token;

				if (!m_vocab.contains(biToken)) {
					continue;
				}
				review.addVectorValue(biToken);

				preToken = token;

			} 
			for (String key : review.m_vector.keySet()) {
				double TF = review.m_vector.get(key).getValue();
				double linearTF = 1 + Math.log10(TF);
				double weight = linearTF * (m_stats.get(key).getValue());
				review.m_vector.get(key).setValue(weight);
			}

		}
	}

	
	public void getQueryReviewWeight() {
		getDocumentWeight(m_reviews);
		System.out.println("get weight for review...");
		getDocumentWeight(Query_reviews);
		System.out.println("get weight for query...");
	}

	public void getSimilarReview(String outSimilarFile) {
		
		System.out.println("get similar review for query...");
		HashMap<Post, HashMap<Post, Double>> qTotalSimilarity = new HashMap<Post, HashMap<Post, Double>>();
		for (Post qReview : Query_reviews) {
			HashMap<Post, Double> Similarity = new HashMap<Post, Double>();
			boolean flag = false;
			double max1 = 0.0;
			double max2 = 0.0;
			double max3 = 0.0;

			Post view1 = null;
			Post view2 = null;
			Post view3 = null;
			
			for (Post mReview : m_reviews) {

					
				double sim = qReview.similiarity(mReview);

				if (!flag) {
					max1 = sim;
					max2 = sim;
					max3 = sim;
					view1 = mReview;
					view2 = mReview;
					view3 = mReview;
					flag = true;
					continue;
				}

				if (sim > max3) {

					if (sim > max2) {
						if (sim > max1) {
							max3 = max2;
							view3 = view2;
							max2 = max1;
							view2 = view1;
							max1 = sim;
							view1 = mReview;
						} else {
								max3 = max2;
								view3 = view2;
							max2 = sim;
							view2 = mReview;
						}
					} else {
						max3 = sim;
						view3 = mReview;
					}

				} else {
					continue;
				}

			}

			System.out.println(max1);
			System.out.println(max2);
			System.out.println(max3);

			Similarity.put(view1, max1);
			Similarity.put(view2, max2);
			Similarity.put(view3, max3);
			
			qTotalSimilarity.put(qReview, Similarity);

		}
		try {
			BufferedWriter out = new BufferedWriter(new FileWriter(outSimilarFile));
		
			for (Post queryKey :qTotalSimilarity.keySet()) {
			
				out.write("........\n");
				out.write("Query" + queryKey.getID());
				out.write("\n");
	
				for (Post similarReview : qTotalSimilarity.get(queryKey).keySet()){
				
					
			
					double sim = qTotalSimilarity.get(queryKey).get(similarReview);
				
					out.write("similarity" + String.valueOf(sim));
					out.write("\n");
					out.write("ID " + similarReview.getID());
					out.write("\n");
					out.write("Author " + similarReview.getAuthor());
					out.write("\n");
					out.write("Content " + similarReview.getContent());
					out.write("\n");
					out.write("Date " + similarReview.getDate());
					out.write("\n");

				
			}
			
		}
		out.flush();
		out.close();
		System.out.println(m_stats.size());
	}catch (IOException e) {
			e.printStackTrace();
		}

			
		}

	public void getBigram(JSONObject json) {
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for (int i = 0; i < jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				

				Set<String> tokenSet = new HashSet<String>();

				String preToken = null;
				boolean flag = false;

				for (String token : tokenizer.tokenize(review.getContent())) {

					Token tokenObject = null;
					token = SnowballStemmingDemo(NormalizationDemo(token));

					if (token.isEmpty()) {
						continue;
					}
					
					if (!m_stats.containsKey(token)) {

						tokenObject = new Token(token);
						int curMapSize = m_stats.size();
						tokenObject.setID(curMapSize + 1);
						m_stats.put(token, tokenObject);

						tokenSet.add(token);
						double curVal = tokenObject.getValue();
						tokenObject.setValue(curVal + 1);
					} else {
						// tokenObject = m_stats.get(token);
						if (!tokenSet.contains(token)) {
							tokenObject = m_stats.get(token);
							tokenSet.add(token);
							double curVal = tokenObject.getValue();
							tokenObject.setValue(curVal + 1);
						}

					}

					if (!flag) {
						preToken = token;
						flag = true;
						continue;
					}

					String biToken = preToken + "_" + token;
					Token biTokenObject = null;
					if (!m_stats.containsKey(biToken)) {

						biTokenObject = new Token(biToken);
						int curMapSize = m_stats.size();
						biTokenObject.setID(curMapSize + 1);
						m_stats.put(biToken, biTokenObject);

						tokenSet.add(biToken);
						double curVal = biTokenObject.getValue();
						biTokenObject.setValue(curVal + 1);
					} else {
						// tokenObject = m_stats.get(token);
						if (!tokenSet.contains(biToken)) {
							biTokenObject = m_stats.get(biToken);
							tokenSet.add(biToken);
							double curVal = biTokenObject.getValue();
							biTokenObject.setValue(curVal + 1);
						}

					}

					preToken = token;
					

				}

				m_reviews.add(review);

			}


		} catch (JSONException e) {
			e.printStackTrace();
		}
	}

	public void getControlledVoc() {
		System.out.println("keySet" + m_stats.keySet().size());
		
		// m_stats.keySet().size()
		Iterator<HashMap.Entry<String, Token>> iter = m_stats.entrySet()
				.iterator();
		while (iter.hasNext()) {
			HashMap.Entry<String, Token> entry = iter.next();
			String key = entry.getKey();
			double tokenDF = entry.getValue().getValue();
			if (tokenDF < 50) {
				iter.remove();
				continue;
			}

			if (key.contains("_")) {
				String[] splitBiToken = key.split("_", 2);
				String uniToken1 = splitBiToken[0];
				String uniToken2 = splitBiToken[1];

				if (m_stopwords.contains(uniToken1)
						|| m_stopwords.contains(uniToken2)) {
					iter.remove();
					continue;
				}

			} else {
				if (m_stopwords.contains(key)) {
					iter.remove();
				}

			}
		}

		m_vocab = m_stats.keySet();
	}

	public void getNewStopWords(String newStopFile) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(newStopFile));
			
			String line = br.readLine();
			
			m_vocab.remove(line);
		} catch (IOException e) {
			System.out.println(e.getMessage());
		}
	}


	public void computeSimilarity(String testfolder, String queryFile,
			String suffix, String stopWordsFile, String newStopFile,
			String outFile) {

		LoadStopwords(stopWordsFile);
		LoadDirectory(testfolder, suffix);
		getControlledVoc();
		getNewStopWords(newStopFile);
		System.out.println("controlled vocabulory");
		computeIDF(); // transfer m_stas into idf hashmap

		loadQuery(queryFile);

		getQueryReviewWeight();

		getSimilarReview(outFile);

		// writeToTxt(outFile);
	}
	
	public void loadQuery(String queryFile) {
		File f = new File(queryFile);
		int size = Query_reviews.size();

				// analyzeDocumentTTF(LoadJson(f.getAbsolutePath()));
				// analyzeDocumentDF(LoadJson(f.getAbsolutePath()));
		JSONObject json = LoadJson(f.getAbsolutePath());
		try {
			JSONArray jarray = json.getJSONArray("Reviews");
			for (int i = 0; i < jarray.length(); i++) {
				Post review = new Post(jarray.getJSONObject(i));
				Query_reviews.add(review);
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		size = Query_reviews.size() - size;
		System.out.println("loadQuery..");
		System.out.println("QuerySize" + size);

	}

	//sample code for loading a json file
	public JSONObject LoadJson(String filename) {
		try {
			BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(filename), "UTF-8"));
			StringBuffer buffer = new StringBuffer(1024);
			String line;
			
			while((line=reader.readLine())!=null) {
				buffer.append(line);
			}
			reader.close();
			
			return new JSONObject(buffer.toString());
		} catch (IOException e) {
			System.err.format("[Error]Failed to open file %s!", filename);
			e.printStackTrace();
			return null;
		} catch (JSONException e) {
			System.err.format("[Error]Failed to parse json file %s!", filename);
			e.printStackTrace();
			return null;
		}
	}
	
	// sample code for demonstrating how to recursively load files in a directory 
	public void LoadDirectory(String folder, String suffix) {
		File dir = new File(folder);
		int size = m_reviews.size();
		for (File f : dir.listFiles()) {
			if (f.isFile() && f.getName().endsWith(suffix)){
				// analyzeDocumentTTF(LoadJson(f.getAbsolutePath()));
				// analyzeDocumentDF(LoadJson(f.getAbsolutePath()));
				analyzeDocumentTTF(LoadJson(f.getAbsolutePath()));
			}
			else if (f.isDirectory())
				LoadDirectory(f.getAbsolutePath(), suffix);
		}
		size = m_reviews.size() - size;
		System.out.println("Loading " + size + " review documents from " + folder);
		System.out.println("word size" + m_stats.size());
	}

	//sample code for demonstrating how to use Snowball stemmer
	public String SnowballStemmingDemo(String token) {
		SnowballStemmer stemmer = new englishStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to use Porter stemmer
	public String PorterStemmingDemo(String token) {
		porterStemmer stemmer = new porterStemmer();
		stemmer.setCurrent(token);
		if (stemmer.stem())
			return stemmer.getCurrent();
		else
			return token;
	}
	
	//sample code for demonstrating how to perform text normalization
	public String NormalizationDemo(String token) {
		// remove all non-word characters
		// please change this to removing all English punctuation
		//token = token.replaceAll("\\W+", ""); 

		token = token.replaceAll("\\p{Punct}+", "");

		// convert to lower case
		token = token.toLowerCase(); 
		
		if (token.matches("[+-]?[0-9]+(\\.[0-9]+)?")) {
			token = "NUM";
		}
		
		token = token.replaceAll("\\W+", "");
		// add a line to recognize integers and doubles via regular expression
		// and convert the recognized integers and doubles to a special symbol "NUM"
		
		return token;
	}
	
	public void TokenizerDemo(String text) {
			/**
			 * HINT: instead of constructing the Tokenizer instance every time when you perform tokenization,
			 * construct a global Tokenizer instance once and evoke it everytime when you perform tokenization.
			 */
			// Tokenizer tokenizer = new TokenizerME(new TokenizerModel(new
			// FileInputStream("./data/Model/en-token.bin")));
			
		System.out
				.format("Token\tNormalization\tSnonball Stemmer\tPorter Stemmer\n");
		for (String token : tokenizer.tokenize(text)) {
			System.out.format("%s\n", token);
			token = token.replaceAll("\\p{Punct}+", "");

			if (token.isEmpty()) {
				System.out.println("empty");
			}
			System.out.println(token.length());
			// System.out.format("%s\t%s\t%s\t%s\n", token,
			// NormalizationDemo(token), SnowballStemmingDemo(token),
			// PorterStemmingDemo(token));
			System.out.format("%s\n", token);
		}
		System.out.println(text);

	}
	
	public void writeToTxt(String fileName) {
		// String fileName = "tokenTTF.txt";
		// String fileName = "tokenDF.txt";
		//String fileName = "tokenDFStopWords.txt";

		try{
			BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
			for (String key : m_stats.keySet()) {
				double TTF = m_stats.get(key).getValue();
	
				out.write(key);
				out.write("	");
				out.write(String.valueOf(TTF));
				out.write('\n');
				
			}
	
			out.flush();
			out.close();
			System.out.println(m_stats.size());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	public static void main(String[] args) {
		try{
			PrintStream out = new PrintStream(new FileOutputStream("mp2_1.txt"));
			System.setOut(out);
		} catch (Exception e) {

		}

		DocAnalyzer analyzer = new DocAnalyzer();
		
		String testfolder = "./data/yelp";
		String queryfile = "./data/query/query.json";
		String suffix = ".json";

		String previousToken = "good";
		int topK = 10;
		//String stopWordsFile = "./data/englishstop.txt";
		//String newStopFile = "./data/newStopWords.txt";
		//String outSimilarFile = "similarReivew.txt";
		//String outIDFFile = "IDF.txt";

		// LoadStopwords(stopWordsFile);
		analyzer.LoadDirectory(testfolder, suffix);
		analyzer.getFollowingToken(previousToken, topK);




	}

}
