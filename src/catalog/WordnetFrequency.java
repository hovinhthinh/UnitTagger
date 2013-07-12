package catalog;

import iitb.shared.EntryWithScore;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Vector;

import org.apache.lucene.analysis.standard.StandardAnalyzer;

import edu.smu.tspell.wordnet.NounSynset;
import edu.smu.tspell.wordnet.Synset;
import edu.smu.tspell.wordnet.SynsetType;
import edu.smu.tspell.wordnet.WordNetDatabase;

public class WordnetFrequency implements WordFrequency {
	public static String quantityTypeString = "how much there is of something that you can quantify";
	public static String quantitySearchString = "quantity";
	public static String calendarMonth = "calendar month";
	NounSynset quantSyn, calenderMonthSyn;
	WordNetDatabase database;
	float stopWordFreq = 0.9f;
	// p -- percent as against poise and poncelet.
	static String stopWords[] = new String[]{"in","are","at","a","from","of","to","the","for","and","with","on","total","per","no","number","amp","apos","quot"};
	static HashSet<String> stopWordsHash=new HashSet<String>(Arrays.asList(stopWords));
	public WordnetFrequency() {
		System.setProperty("wordnet.database.dir", "/mnt/b100/d0/library/public_html/wordnet/WordNet-2.1/dict");
		database = WordNetDatabase.getFileInstance();
		Synset syns[] = database.getSynsets(quantitySearchString, SynsetType.NOUN);
		quantSyn = (NounSynset) syns[0];
		calenderMonthSyn = (NounSynset) database.getSynsets(calendarMonth, SynsetType.NOUN)[0];
	}
	public boolean isUnit(NounSynset nsyn) {
		// over-generalizes for words like last,span
		// Quantity hypernym includes way too many units.
		for  (int path = 0; nsyn != null; path++) {
			NounSynset hypos[] = nsyn.getHypernyms();
			nsyn = null;
			for (int h = 0; h < hypos.length; h++) {
				nsyn = hypos[0];
				if (hypos[h] == calenderMonthSyn) {
					// ensures that names of months are not marked as units.
					return false;
				}
				if (hypos[h]==quantSyn) {
					return true;
				}
			}
		}
		return false;
	}
	public void test(String args[]) {
		if (args.length > 0) {
			for (String wordForm : args) {
				//  Get the synsets containing the word form
				Synset[] synsets = database.getSynsets(wordForm, SynsetType.NOUN);
				//  Display the word forms and definitions for synsets retrieved
				if (synsets.length > 0)
				{
					System.out.println("Tag count for each synset:");
					for (int i = 0; i < synsets.length; i++)
					{
						System.out.println("");
						String[] wordForms= synsets[i].getWordForms();
						SynsetType type = synsets[i].getType();
						for(int j=0;j<wordForms.length;j++){
							System.out.println(wordForms[j]+" [" + synsets[i].getDefinition() + "] " + synsets[i].getTagCount(wordForms[j]));
						}
						if (synsets[i] instanceof NounSynset) {
							NounSynset nsyn = (NounSynset) synsets[i];

						}
					}
				}
				else
				{
					System.err.println("No synsets exist that contain " +
							"the word form '" + wordForm + "'");
				}
			}
		}
		else
		{
			System.err.println("You must specify " +
			"a word form for which to retrieve synsets.");
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		//args = HeaderSegmenter.WordSymbols;
		args = new String[]{"period"};
		WordnetFrequency wordFreq = new WordnetFrequency();
		List<EntryWithScore<String[]>> matches = new Vector<EntryWithScore<String[]>>();
		wordFreq.getRelativeFrequency(args[0], matches);
		System.out.println(Arrays.toString(matches.toArray()));
		wordFreq.test(args);
	}

	@Override
	public boolean getRelativeFrequency(String wordForm, List<EntryWithScore<String[]>> matches) {
		Synset[] synsets = database.getSynsets(wordForm, SynsetType.NOUN);
		//  Display the word forms and definitions for synsets retrieved
		matches.clear();
		int total = 0;
		for (int i = 0; i < synsets.length; i++)
		{
			String[] wordForms= synsets[i].getWordForms();
			int cnt = 1;
			for(int j=0;j<wordForms.length;j++){
				cnt += synsets[i].getTagCount(wordForms[j]);
			}
			if (isUnitDefn((NounSynset) synsets[i])) {
				matches.add(new EntryWithScore<String[]>(wordForms, cnt));
			}
			/*if (isUnit((NounSynset) synsets[i]) != isUnitDefn(synsets[i])) {
					System.out.println(wordForm + " " + synsets[i]);
					System.out.println();
				}*/
			total += cnt;

		}
		boolean foundMatch = synsets.length > 0;
		if (stopWordsHash.contains(wordForm)) {
			total /= (1-stopWordFreq);
			foundMatch=true;
		}
		for (int i = 0; i < matches.size(); i++) {
			matches.get(i).setScore(matches.get(i).getScore()/total);
		}
		//if (foundMatch && matches.size()==0) {
		//	matches.add(new EntryWithScore<String[]>(new String[]{wordForm}, 1e-6));
		//}
		return foundMatch;
	}
	private boolean isUnitDefn(Synset synset) {
		// was "period of time" earlier and did not capture words like week.
		return (synset.getDefinition().contains("unit") || (synset.getDefinition().contains("period of")));
	}
}