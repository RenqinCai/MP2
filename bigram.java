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
