
/**
 * PageRank Project (Section 1)
 * 
 * 1. Kittinun Aukkapinyo	5888006
 * 2. Chatchawan Kotarasu	5888084
 * 3. Thatchapon Unprasert	5888220
 * 
 */

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class implements PageRank algorithm on simple graph structure. Put your name(s), ID(s), and section here.
 *
 */
public class PageRanker {

	/*
	 * To simplify our data structure, we chose Node as a direct graph representation that coordinates better
	 * relationship between nodes (or pages). Specifying in-links and out-links are therefore simple. Data structure for
	 * those links are determined to be sets because it efficiently solves duplication issue.
	 * 
	 */
	class Node implements Comparable<Node> {
		public Integer pid;
		public Set<Node> in;
		public Set<Node> out;
		public Double score;
		public Double newScore;
		public double iOutSize;

		public Node(Integer id) {
			pid = id;
			in = new HashSet<Node>();
			out = new HashSet<Node>();
		}

		public void addIn(Node in) {
			this.in.add(in);
		}

		public void addOut(Node out) {
			this.out.add(out);
		}

		public boolean isSink() {
			// By definition of a sink page whose out-links are none
			return out.isEmpty();
		}
		
		public int compareTo(Node n) {
			return pid.compareTo(n.pid);
		}
	}

	// Conventional damping factor
	private static final double d = 0.85;
	// Inverse of log2 to save up computation time from time-consuming log2 division
	private static final double ilog2 = 1.0 / Math.log(2);

	// Data structure to store all pages associated with their page number
	private Map<Integer, Node> graph;
	// Data structure to store all sink pages
	private Vector<Node> S;
	// Integer to store floor of previous perplexity value
	private int prevUnit;
	// Integer to store iterations of "similar" perplexity value
	private int iteration;
	// Double to store perplexity value for convergence determination
	private double perplexity;
	// Double to cache 1/N
	private Double iN;

	// Writers and string builders for output files
	private BufferedWriter perplexityWriter;
	private BufferedWriter scoreWriter;
	private StringBuilder perplexityBuilder;
	private StringBuilder scoreBuilder;

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
			StringTokenizer token;
			graph = new HashMap<Integer, Node>();
			while ((line = br.readLine()) != null) {
				token = new StringTokenizer(line, " ");
				// Always read the first page, put it into the graph if necessary
				Node node = graph.get(pid = parseInt(token.nextToken()));
				if (node == null)
					graph.put(pid, node = new Node(pid));
				// Read its in-pages, if any, put them into the graph if necessary
				while (token.hasMoreTokens()) {
					Node inNode = graph.get(pid = parseInt(token.nextToken()));
					if (inNode == null)
						graph.put(pid, inNode = new Node(pid));
					// Out-page of all in-pages is the first page in the line
					inNode.addOut(node);
					// Link in-pages to the first page in the line
					node.addIn(inNode);
				}
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Redefined parseInt() function that is faster than the original because it assumes seeing only positive integers.
	 * 
	 * @param s
	 * @return
	 */
	public static Integer parseInt(final String s) {
		final int len = s.length();
		Integer num = '0' - s.charAt(0);
		int i = 1;
		while (i < len)
			num = num * 10 + '0' - s.charAt(i++);
		return -num;
	}

	/**
	 * This method will be called after the graph is loaded into the memory. This method initialize the parameters for
	 * the PageRank algorithm including setting an initial weight to each page.
	 */
	public void initialize() {
		// General variables initialization
		S = new Vector<Node>();
		iN = 1.0 / graph.size();
		for (Node node : graph.values()) {
			node.score = iN; // Initial score for each page: 1/N
			if (node.isSink())
				S.add(node); // Add sink pages here
			else
				node.iOutSize = 1.0 / node.out.size(); // Store 1/L(q) (from pseudocode) for faster calculation
		}
		S.trimToSize();
	}

	/**
	 * Computes the perplexity of the current state of the graph. The definition of perplexity is given in the project
	 * specs.
	 */
	public double getPerplexity() {
		// We calculate this value in runPageRank()
		return perplexity;
	}

	/**
	 * Returns true if the perplexity converges (hence, terminate the PageRank algorithm). Returns false otherwise (and
	 * PageRank algorithm continue to update the page scores).
	 */
	public boolean isConverge() {
		// Each time, write out perplexity value
		double perplexity = getPerplexity();
		perplexityBuilder.append(perplexity);
		perplexityBuilder.append("\n");
		int unit = (int) perplexity;
		// When no much change in perplexity for 4 times, convergence is assumed
		if (unit == prevUnit && ++iteration == 4)
			return true;
		// Otherwise, reset the counter
		if (unit != prevUnit)
			iteration = 1;
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
		// Perform the given pseudocode, with a number of optimizations to reduce as many as possible computation
		// operations
		double sinkPR, newPRVal, newPRVal2;
		double dN = d * iN;
		double idN = iN - dN;
		Double order;
		perplexityBuilder = new StringBuilder();
		scoreBuilder = new StringBuilder();
		do {
			sinkPR = 0.0;
			for (Node node : S)
				sinkPR += node.score;
			newPRVal = idN + sinkPR * dN; // This value is a starting for every page, calculated once
			order = 0.0;
			for (Node p : graph.values()) {
				if (p.in.isEmpty())
					p.newScore = newPRVal; // newPRVal2 is 0, don't even need to include here
				else {
					newPRVal2 = 0.0; // This value is later multiplied by d
					for (Node q : p.in)
						// Performance: Multiplication with 1/L(q) > Division with L(q)
						newPRVal2 += q.score * q.iOutSize;
					p.newScore = newPRVal + d * newPRVal2;
				}
				// Computing log base 2, but multiplication of log2 inverse for better performance
				order += p.newScore * Math.log(p.newScore) * ilog2;
			}
			for (Node p : graph.values())
				p.score = p.newScore; // Update "PR" from "newPR"
			// TODO: Alternatively, the faster exp(-order) (hence ignoring ilog2) may be used, but precision may
			// oscillate in a worse way
			perplexity = Math.pow(2, -order);
		} while (!isConverge());
		try {
			// Write out perplexity values and PageRank scores (PR)
			perplexityWriter = new BufferedWriter(new FileWriter(perplexityOutFilename));
			perplexityWriter.write(perplexityBuilder.toString());
			perplexityWriter.close();
			scoreWriter = new BufferedWriter(new FileWriter(prOutFilename));
			// Sort the pages before writing out their PR score
			Vector<Node> nodes = new Vector<Node>(graph.values());
			Collections.sort(nodes);
			for (Node node : nodes) {
				scoreBuilder.append(node.pid);
				scoreBuilder.append(" ");
				scoreBuilder.append(node.score);
				scoreBuilder.append("\n");
			}
			scoreWriter.write(scoreBuilder.toString());
			scoreWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Return the top K page IDs, whose scores are highest.
	 */
	public Integer[] getRankedPages(int K) {
		// Wrap the graph key set with a list to be sorted using PageRank score of each page number
		List<Integer> pages = new Vector<Integer>(graph.keySet());
		Collections.sort(pages, new Comparator<Integer>() {
			@Override
			public int compare(Integer p1, Integer p2) {
				return graph.get(p2).score.compareTo(graph.get(p1).score);
			}
		});
		// Make sure |output| <= K
		if (pages.size() > K)
			pages = pages.subList(0, K);
		return pages.toArray(new Integer[Math.min(K, pages.size())]);
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
