import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Vector;

/**
 * This class implements PageRank algorithm on simple graph structure. Put your name(s), ID(s), and section here.
 *
 */
public class PageRanker {
	
	class Node {
		private int pid;
		private List<Node> in;
		private List<Node> out;
		
		public Node(int id) {
			pid = id;
			in = new Vector<Node>();
			out = new Vector<Node>();
		}
		
		public int getPid() {
			return pid;
		}
		
		public List<Node> getIn() {
			return in;
		}
		
		public List<Node> getOut() {
			return out;
		}

		public void addIn(Node in) {
			this.in.add(in);
		}

		public void addOut(Node out) {
			this.out.add(out);
		}
		
		public boolean isSink() {
			return out.isEmpty();
		}
	}

	private static final double d = 0.85;

	private TreeMap<Integer, Node> graph;
	private List<Integer> S;
	private HashMap<Integer, Double> PR;
	private int prevUnit = -1;
	private int iteration = 0;

	/**
	 * This class reads the direct graph stored in the file "inputLinkFilename" into memory. Each line in the input file
	 * should have the following format: <pid_1> <pid_2> <pid_3> .. <pid_n>
	 * 
	 * Where pid_1, pid_2, ..., pid_n are the page IDs of the page having links to page pid_1. You can assume that a
	 * page ID is an integer.
	 */
	public void loadData(String inputLinkFilename) {
		try {
			BufferedReader br = new BufferedReader(new FileReader(inputLinkFilename));
			String line;
			Integer pid;
			graph = new TreeMap<Integer, Node>();
			S = new Vector<Integer>();
			System.out.println("DEBUG: File reading...");
			while ((line = br.readLine()) != null) {
				String tokens[] = line.split(" ");
				Node node = graph.get(pid = Integer.parseInt(tokens[0]));
				if (node == null)
					graph.put(pid, node = new Node(pid));
				for (int i = 1; i < tokens.length; i++) {
					Node inNode = graph.get(pid = Integer.parseInt(tokens[i]));
					if (inNode == null)
						graph.put(pid, inNode = new Node(pid));
					inNode.addOut(node);
					node.addIn(inNode);
				}
				tokens = null;
			}
			for (Node node : graph.values()) {
				if (node.isSink())
					S.add(node.pid);
			}
			br.close();
		} catch (IOException e) {

		}
	}

	/**
	 * This method will be called after the graph is loaded into the memory. This method initialize the parameters for
	 * the PageRank algorithm including setting an initial weight to each page.
	 */
	public void initialize() {
		PR = new HashMap<Integer, Double>(graph.size());
		double iN = 1.0 / graph.size();
		for (Integer pid : graph.keySet())
			PR.put(pid, iN);
	}

	/**
	 * Computes the perplexity of the current state of the graph. The definition of perplexity is given in the project
	 * specs.
	 */
	public double getPerplexity() {
		double order = 0;
		double log2 = Math.log(2);
		for (Integer pid : graph.keySet())
			order += PR.get(pid) * Math.log(PR.get(pid));
		return Math.pow(2, -order / log2);
	}

	/**
	 * Returns true if the perplexity converges (hence, terminate the PageRank algorithm). Returns false otherwise (and
	 * PageRank algorithm continue to update the page scores).
	 */
	public boolean isConverge() {
		double perplexity = getPerplexity();
		int unit = (int) perplexity;
		System.out.println("P " + unit);
		if (unit == prevUnit && ++iteration >= 4)
			return true;
		iteration = 0;
		prevUnit = unit;
		return false;
	}

	/**
	 * The main method of PageRank algorithm. Can assume that initialize() has been called before this method is
	 * invoked. While the algorithm is being run, this method should keep track of the perplexity after each iteration.
	 * 
	 * Once the algorithm terminates, the method generates two output files. [1] "perplexityOutFilename" lists the
	 * perplexity after each iteration on each line. The output should look something like:
	 * 
	 * 183811 79669.9 86267.7 72260.4 75132.4
	 * 
	 * Where, for example,the 183811 is the perplexity after the first iteration.
	 *
	 * [2] "prOutFilename" prints out the score for each page after the algorithm terminate. The output should look
	 * something like:
	 * 
	 * 1 0.1235 2 0.3542 3 0.236
	 * 
	 * Where, for example, 0.1235 is the PageRank score of page 1.
	 * 
	 */
	public void runPageRank(String perplexityOutFilename, String prOutFilename) {
		double sinkPR;
		Map<Integer, Double> newPR;
		int N = graph.size();
		double dN = (1 - d) / N;
		while (!isConverge()) {
			System.out.println("DEBUG: Converging...");
			sinkPR = 0;
			for (Integer pid : S)
				sinkPR += PR.get(pid);
			newPR = new HashMap<Integer, Double>(PR);
			for (Node p : graph.values()) {
				newPR.put(p.pid, dN + (d * sinkPR / N));
				for (Node q : p.getIn()) {
					double newPRValAdd = newPR.get(p.pid) + (d * PR.get(q.pid) / q.getOut().size());
					newPR.put(p.pid, newPRValAdd);
				}
			}
			for (Integer pid : graph.keySet())
				PR.put(pid, newPR.get(pid));
			newPR = null;
		}
	}

	/**
	 * Return the top K page IDs, whose scores are highest.
	 */
	public Integer[] getRankedPages(int K) {
		Integer[] rankedPages = new Integer[graph.size()];
		for (int i = 0; i < graph.size(); i++)
			rankedPages[i] = i + 1;
		Arrays.sort(rankedPages, new Comparator<Integer>() {
			@Override
			public int compare(Integer p1, Integer p2) {
				return (int) (PR.get(p2) - PR.get(p1));
			}
		});
		rankedPages[K] = null;
		return rankedPages;
	}

	public static void main(String args[]) {
		long startTime = System.currentTimeMillis();
		PageRanker pageRanker = new PageRanker();
		pageRanker.loadData("citeseer.dat");
		pageRanker.initialize();
		pageRanker.runPageRank("perplexity.out", "pr_scores.out");
		Integer[] rankedPages = pageRanker.getRankedPages(100);
		double estimatedTime = (double) (System.currentTimeMillis() - startTime) / 1000.0;

		System.out.println("Top 100 Pages are:\n" + Arrays.toString(rankedPages));
		System.out.println("Proccessing time: " + estimatedTime + " seconds");
	}
}
