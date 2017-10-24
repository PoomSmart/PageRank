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
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;

/**
 * This class implements PageRank algorithm on simple graph structure. Put your name(s), ID(s), and section here.
 *
 */
public class PageRanker {

	class Node {
		public Integer pid;
		public Set<Node> in;
		public Set<Node> out;
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
			return out.isEmpty();
		}
	}

	private static final boolean debug = false;

	private static final double d = 0.85;
	private static final double ilog2 = 1.0 / Math.log(2);

	private Map<Integer, Node> graph;
	private Vector<Integer> S;
	private Map<Integer, Double> PR;
	private int prevUnit;
	private int iteration;
	private double perplexity;
	private Double iN;

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
			if (debug)
				System.out.println("DEBUG: File reading...");
			while ((line = br.readLine()) != null) {
				token = new StringTokenizer(line, " ");
				Node node = graph.get(pid = parseInt(token.nextToken()));
				if (node == null)
					graph.put(pid, node = new Node(pid));
				while (token.hasMoreTokens()) {
					Node inNode = graph.get(pid = parseInt(token.nextToken()));
					if (inNode == null)
						graph.put(pid, inNode = new Node(pid));
					inNode.addOut(node);
					node.addIn(inNode);
				}
			}
			if (debug)
				System.out.println("DEBUG: Graph size = " + graph.size());
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

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
		PR = new HashMap<Integer, Double>(graph.size());
		S = new Vector<Integer>();
		iN = 1.0 / graph.size();
		for (Node node : graph.values()) {
			PR.put(node.pid, iN);
			if (node.isSink())
				S.add(node.pid);
			else
				node.iOutSize = 1.0 / node.out.size();
		}
		S.trimToSize();
	}

	/**
	 * Computes the perplexity of the current state of the graph. The definition of perplexity is given in the project
	 * specs.
	 */
	public double getPerplexity() {
		return perplexity;
	}

	/**
	 * Returns true if the perplexity converges (hence, terminate the PageRank algorithm). Returns false otherwise (and
	 * PageRank algorithm continue to update the page scores).
	 */
	public boolean isConverge() {
		double perplexity = getPerplexity();
		perplexityBuilder.append(perplexity);
		perplexityBuilder.append("\n");
		int unit = (int) perplexity;
		if (debug) {
			System.out.println("DEBUG: Perplexity = " + perplexity);
			System.out.println("DEBUG: Iteration = " + iteration);
		}
		if (unit == prevUnit && ++iteration == 4)
			return true;
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
		double sinkPR, newPRVal, newPRVal2;
		Map<Integer, Double> newPR = new HashMap<Integer, Double>(PR.size());
		double dN = d * iN;
		double idN = iN - dN;
		Double order, value;
		perplexityBuilder = new StringBuilder();
		scoreBuilder = new StringBuilder();
		do {
			if (debug)
				System.out.println("DEBUG: Converging...");
			sinkPR = 0.0;
			for (Integer pid : S)
				sinkPR += PR.get(pid);
			newPRVal = idN + sinkPR * dN;
			for (Node p : graph.values()) {
				if (p.in.isEmpty())
					newPR.put(p.pid, newPRVal);
				else {
					newPRVal2 = 0.0;
					for (Node q : p.in)
						newPRVal2 += PR.get(q.pid) * q.iOutSize;
					newPR.put(p.pid, newPRVal + d * newPRVal2);
				}
			}
			order = 0.0;
			for (Entry<Integer, Double> entry : newPR.entrySet()) {
				value = entry.getValue();
				order += value * Math.log(value) * ilog2;
				PR.put(entry.getKey(), value);
			}
			perplexity = Math.pow(2, -order);
		} while (!isConverge());
		try {
			perplexityWriter = new BufferedWriter(new FileWriter(perplexityOutFilename));
			perplexityWriter.write(perplexityBuilder.toString());
			perplexityWriter.close();
			scoreWriter = new BufferedWriter(new FileWriter(prOutFilename));
			for (Entry<Integer, Double> entry : PR.entrySet()) {
				scoreBuilder.append(entry.getKey());
				scoreBuilder.append(" ");
				scoreBuilder.append(entry.getValue());
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
		List<Integer> pages = new Vector<Integer>(graph.keySet());
		Collections.sort(pages, new Comparator<Integer>() {
			@Override
			public int compare(Integer p1, Integer p2) {
				return PR.get(p2).compareTo(PR.get(p1));
			}
		});
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
