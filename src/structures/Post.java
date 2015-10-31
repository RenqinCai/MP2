/**
 * 
 */
package structures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import json.JSONException;
import json.JSONObject;

/**
 * @author hongning
 * @version 0.1
 * @category data structure
 * data structure for a Yelp review document
 * You can create some necessary data structure here to store the processed text content, e.g., bag-of-word representation
 */
public class Post {
	//unique review ID from Yelp
	String m_ID;	
	
	public void setID(String ID) {
		m_ID = ID;
	}
	
	public String getID() {
		return m_ID;
	}

	//author's displayed name
	String m_author;
	
	public String getAuthor() {
		return m_author;
	}

	public void setAuthor(String author) {
		this.m_author = author;
	}
	
	//author's location
	String m_location;
	public String getLocation() {
		return m_location;
	}

	public void setLocation(String location) {
		this.m_location = location;
	}

	//review text content
	String m_content;
	public String getContent() {
		return m_content;
	}

	public void setContent(String content) {
		if (!content.isEmpty())
			this.m_content = content;
	}
	
	public boolean isEmpty() {
		return m_content==null || m_content.isEmpty();
	}

	//timestamp of the post
	String m_date;
	public String getDate() {
		return m_date;
	}

	public void setDate(String date) {
		this.m_date = date;
	}
	
	//overall rating to the business in this review
	double m_rating;	

	public double getRating() {
		return m_rating;
	}

	public void setRating(double rating) {
		this.m_rating = rating;
	}

	public Post(String ID) {
		m_ID = ID;
	}
	


	public HashMap<String, Token> m_vector; // suggested sparse structure for
											// storing
										// the vector space representation with
										// N-grams for this document
	
	public double similiarity(Post p) {
		Set<String> commonKeySet = new HashSet<String>(m_vector.keySet());
		commonKeySet.retainAll(p.m_vector.keySet());
		
		double sim = 0.0; 
		double vector_norm = 0.0;
		double pvector_norm = 0.0;
		
		for (String key : commonKeySet) {
			double m_weight = m_vector.get(key).m_value;
			double p_weight = p.m_vector.get(key).m_value;
			
			sim += m_weight * p_weight;
		}

		for (String key : p.m_vector.keySet()) {
			pvector_norm += Math.pow(p.m_vector.get(key).m_value, 2);
		}

		for (String key : m_vector.keySet()) {
			vector_norm += Math.pow(m_vector.get(key).m_value, 2);
		}
		
		vector_norm = Math.sqrt(vector_norm);
		pvector_norm = Math.sqrt(pvector_norm);
		sim = sim / (vector_norm * pvector_norm);

		return sim;// compute the cosine similarity between this post and input
					// p based on their vector space representation
	}

	// add term frequency in a document
	public void addVectorValue(String key) {
		if(m_vector.containsKey(key)){
			Token tokenObject = m_vector.get(key);
			double curTokenVal = tokenObject.getValue() + 1;
			tokenObject.setValue(curTokenVal);
			
		} else {
			Token tokenObject = new Token(key);
			double curTokenVal = 1;
			tokenObject.setValue(curTokenVal);
			m_vector.put(key, tokenObject);
		}
		
		
	}
	
	// public void getVectorNorm() {
	// for (String key : m_vector.keySet()) {
	// vector_norm
	// }
	// }

	public Post(JSONObject json) {
		try {
			m_vector = new HashMap<String, Token>();
			m_ID = json.getString("ReviewID");
			setAuthor(json.getString("Author"));
			
			setDate(json.getString("Date"));			
			setContent(json.getString("Content"));
			setRating(json.getDouble("Overall"));
			setLocation(json.getString("Author_Location"));			
		} catch (JSONException e) {
			e.printStackTrace();
		}
	}
	
	public JSONObject getJSON() throws JSONException {
		JSONObject json = new JSONObject();
		
		json.put("ReviewID", m_ID);//must contain
		json.put("Author", m_author);//must contain
		json.put("Date", m_date);//must contain
		json.put("Content", m_content);//must contain
		json.put("Overall", m_rating);//must contain
		json.put("Author_Location", m_location);//must contain
		
		return json;
	}
}
