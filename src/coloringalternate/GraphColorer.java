package coloringalternate;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.LinkedList;
import java.util.Scanner;

import representations.GraphMatrixGenerator;

public class GraphColorer implements Runnable {

	public static void main(String[] args) {

		// LOAD DATA

		graph = null;

		if (args.length == 0) {
			// Testing params
			int size = 100;
			float connectedness = 0.5f;

			int[][] graphmatrix_int = GraphMatrixGenerator.genUndirectedGraph(size, connectedness);
			if (size < 75)
				GraphMatrixGenerator.printMatrix(graphmatrix_int);

			boolean[][] adj_matrix = new boolean[size][size];
			for (int i = 0; i < size; i++) {
				for (int j = 0; j < size; j++) {
					if (graphmatrix_int[i][j] == 1)
						adj_matrix[i][j] = true;
				}
			}
			graphmatrix_int = null;
			System.gc();
			graph = new GraphData(adj_matrix);

		} else {
			String path = args[0];
			try {
				Scanner sc = new Scanner(new FileInputStream(new File(path)));
				String[] firstline = sc.nextLine().split(" ");

				int nodes = Integer.parseInt(firstline[0]);
				int edges = Integer.parseInt(firstline[1]);

				boolean[][] adj_matrix = new boolean[nodes][nodes];

				for (int i = 0; i < edges; i++) {
					String[] line = sc.nextLine().split(" ");
					adj_matrix[Integer.parseInt(line[0])][Integer.parseInt(line[1])] = true;
					adj_matrix[Integer.parseInt(line[1])][Integer.parseInt(line[0])] = true;
				}
				graph = new GraphData(adj_matrix);

				sc.close();

			} catch (FileNotFoundException e) {
				e.printStackTrace();
				System.exit(0);
			}
		}

		// END LOAD DATA

		current_crd = new ColoringReturnData(0, null);

		Thread thread = null;

		max_colors = graph.MAX_DEGREE + 1; // Start off with max degree
											// colors

		int[] final_output = new int[graph.VERTICES];
		for (int i = 0; i < graph.VERTICES; i++) {
			final_output[i] = i;
		}
		while (current_crd.status == 0) {
			thread = new Thread(new GraphColorer(), "Thread");
			System.out.println(max_colors + "?");
			current_crd.status = 1;
			long t0 = System.currentTimeMillis();
			thread.setPriority(Thread.MAX_PRIORITY);
			thread.start();
			while (thread.isAlive() && System.currentTimeMillis() - 1800 * 1000 < t0) {

			}
			if (current_crd.status == 0) {
				final_output = current_crd.getColors().clone();
			}
			int highest_color = 0;
			for (int i : final_output) {
				if (i > highest_color)
					highest_color = i;
			}
			max_colors = highest_color;
		}
		// Loop until fail TODO
		// Keep best solution saved
		// Use a global variable - access from thread

		// Submits solution and checks validity
		int maxColor = 0;
		for (int i = 0; i < final_output.length; i++) {
			if (maxColor < final_output[i])
				maxColor = final_output[i];
		}
		System.out.println((maxColor + 1) + " 0");

		for (int i = 0; i < final_output.length; i++) {

			System.out.print(final_output[i] + " ");
		}

		for (int i = 0; i < graph.VERTICES - 1; i++) {
			for (int j = i + 1; j < graph.VERTICES; j++) {
				if (final_output[i] == final_output[j] && graph.ADJ_MAT[i][j]) {
					System.out.print("\nCollision @ vertices " + i + ", " + j);
				}
			}
		}

		System.exit(0);

	}

	private static ColoringReturnData current_crd;
	private static GraphData graph;

	// Data structure to hold the graph data
	private static class GraphData {
		final boolean[][] ADJ_MAT; // Adjacency matrix
		final int[] DEGREE; // the degree of each vertex
		final int MAX_DEGREE; // maximum degree of the graph
		final int VERTICES; // number of vertices
		// CONSTANTS therefore final

		public GraphData(boolean[][] matrix) {
			ADJ_MAT = matrix;
			VERTICES = ADJ_MAT.length;
			DEGREE = GraphColorer.getDegreeArray(matrix);
			int max_degree = 0;
			for (int i : DEGREE) {
				max_degree = (max_degree < i) ? i : max_degree;
			}
			MAX_DEGREE = max_degree;

			// TODO graph data - similar vertices?
		}

	}

	// A data structure to pass solutions (or lack thereof)
	private static class ColoringReturnData {

		int status;
		private int[] colors;
		int num_colors;

		ColoringReturnData(int status, int[] colors) {

			this.status = status;
			this.setColors(colors);
			updateNumColors();
		}

		private void updateNumColors() { //
			num_colors = 0;
			if (colors != null) {
				for (int c : colors) {
					if (c > num_colors) {
						num_colors = c;
					}
				}
				num_colors++;
			}
		}

		public int[] getColors() {
			return colors;
		}

		public void setColors(int[] colors) {
			this.colors = colors;
			updateNumColors();
		}

	}

	private class SolverFrame {
		int[] coloring;
		boolean[] colored_verts;
		LinkedList<Integer>[] domain;
		int smallest_unused_color;
		int next_vertex;

		int setNextVertex() {
			int index = -1;
			for (int i = 0; i < graph.VERTICES; i++) {
				if (index == -1) {
					if (!colored_verts[i]) {
						index = i;
					}
				} else if (!colored_verts[i] && ((domain[index].size() > domain[i].size())
						| (domain[index].size() >= domain[i].size() && graph.DEGREE[index] < graph.DEGREE[i])))
					index = i;
			}
			next_vertex = index;
			if (index != -1 && smallest_unused_color + 1 < max_colors) {
				for (int i = domain[next_vertex].size() - 1; i > 0; i++) {
					if (domain[next_vertex].getLast() > smallest_unused_color) {
						domain[next_vertex].removeLast();
					} else
						break;
				}
			}
			return index;
		}

		void deinitialize() {
			for (LinkedList<Integer> d : domain) {
				d.clear();
				colored_verts = null;
				coloring = null;
			}
		}

		public SolverFrame(int[] coloring, LinkedList<Integer>[] domain, boolean[] colored_verts) {
			this.coloring = coloring.clone();
			if (domain == null) {
				initializeDomains();
			} else {
				this.domain = new LinkedList[graph.VERTICES];
				for (int i = 0; i < graph.VERTICES; i++) {
					this.domain[i] = (LinkedList<Integer>) domain[i].clone();
				}
			}

			this.colored_verts = colored_verts.clone();
		}

		/*
		 * max_colors should be specified in the solve method. This method will
		 * be called for the first SolverFrame. The others will modify their own
		 * copy of the domain based on this one:
		 */
		void initializeDomains() {
			domain = new LinkedList[graph.VERTICES];
			for (int i = 0; i < graph.VERTICES; i++) {
				domain[i] = new LinkedList<Integer>();
				for (int j = 0; j <= graph.DEGREE[i] && j < max_colors; j++) {
					domain[i].add(j);
				}
			}
			boolean b = true;
			while (b) {
				b = false;
				for (int i = 0; i < graph.VERTICES; i++) {
					int max_of_neighbor = 0;
					if (graph.DEGREE[i] > 3) {
						for (int j = 0; j < graph.VERTICES; j++)
							if (graph.ADJ_MAT[i][j])
								for (Integer d : domain[j])
									if (max_of_neighbor < d)
										max_of_neighbor = d;
					}
					int init_size = domain[i].size();
					while (domain[i].getLast() > max_of_neighbor+1) {
						domain[i].removeLast();
					}
					
					if (domain[i].size() < init_size) {
						System.out.print((init_size - domain[i].size())+ " domain slots saved ");
						b = true;
					}
				}
			}
			System.out.println("initialization of domains done");
		}

		void colorVertex(int vertex) {
			try {
				if (domain[vertex].getFirst() <= smallest_unused_color) {
					int color = domain[vertex].getFirst();
					if (color == smallest_unused_color) {
						smallest_unused_color++;
					}
					coloring[vertex] = color;
					colored_verts[vertex] = true;

					domain[vertex].clear();

					for (int j = 0; j < graph.VERTICES; j++) {
						if (graph.ADJ_MAT[vertex][j] && domain[j].contains(color)) {
							domain[j].removeFirstOccurrence(color);
						}
					}
				} else {
					backtrack_flag = true;
				}

			} catch (Exception e) {
			}
			boolean comp = true;
			for (int i = 0; i < graph.VERTICES; i++) {
				if (!colored_verts[i]) {
					comp = false;
					break;
				}
			}
			if (comp) {
				ColoringReturnData other_crd = new ColoringReturnData(0, coloring.clone());
				GraphColorer.setValuesInCRD(test_crd, other_crd);
			}

		}

		void handleDefinites() {
			// If similar vertices are implemented, do not modify this method,
			// as it will apply automatically to similar vertices
			for (int i = 0; i < graph.VERTICES; i++) {
				if (colored_verts[i] == false && domain[i].size() == 1) {
					coloring[i] = domain[i].getFirst();
					if (smallest_unused_color == coloring[i]) {
						smallest_unused_color++;
					}
					colored_verts[i] = true;
					
					for (int j = 0; j < graph.VERTICES; j++) {
						if (domain[j].isEmpty()) {
							backtrack_flag = true;
							return;
						}
						if (graph.ADJ_MAT[i][j] && !colored_verts[j])
							domain[j].removeFirstOccurrence(domain[i].getFirst());
					}
					domain[i].clear();
				}
			}
		}

		void removeFirstInDomain(int vertex) {
			try {
				domain[vertex].remove();
			} catch (Exception e) {

			}
		}

		private boolean areDomainsOpen() {
			// Checks if any uncolored vertex' domain has size 0
			for (int i = 0; i < graph.VERTICES; i++) {
				if (!colored_verts[i] && domain[i].isEmpty()) {
					return false;
				}
			}
			return true;
		}

	}

	int[] ordering;
	static int max_colors;
	SolverFrame[] stack;
	boolean complete;
	ColoringReturnData test_crd;
	boolean backtrack_flag;
	int pointer;

	private void solve() {

		test_crd = new ColoringReturnData(1, null);
		backtrack_flag = false;

		complete = false;

		ordering = new int[graph.VERTICES];
		for (int i = 0; i < ordering.length; i++) {
			ordering[i] = -1;
		}

		int[] blank_coloring = new int[graph.VERTICES];
		boolean[] not_colored = new boolean[graph.VERTICES];

		pointer = -1;
		stack = new SolverFrame[graph.VERTICES + 1];

		// Push blank frame onto stack
		stack[++pointer] = new SolverFrame(blank_coloring, null, not_colored);

		while (pointer != -1 && !complete) {
			if (!stack[pointer].areDomainsOpen() || backtrack_flag) { // If one
																		// of
																		// domains
																		// empty
				backtrack_flag = false;
				test_crd.status = 1;
				stack[pointer].deinitialize();
				stack[pointer] = null; // destroy stack at pointer

				pointer--; // decrement pointer
				if (pointer != -1 && !complete) { // if not in terminating
													// condition
					//
					// Remove element of this domain leading to following
					// frame having an invalid state
					stack[pointer].removeFirstInDomain(ordering[pointer]);

				}
			} else {

				if (!complete) {
					// Push onto stack with updated data
					stack[++pointer] = new SolverFrame(stack[pointer - 1].coloring, stack[pointer - 1].domain,
							stack[pointer - 1].colored_verts);
					stack[pointer].smallest_unused_color = stack[pointer - 1].smallest_unused_color;
					// Try coloring a vertex in the stack then handle some definites that arise from that coloring
					stack[pointer].colorVertex(stack[pointer - 1].setNextVertex());
					stack[pointer].handleDefinites();
					ordering[pointer - 1] = stack[pointer - 1].next_vertex;
					if (test_crd.status == 0) {
						setValuesInCRD(current_crd, test_crd);
						complete = true;
					}
					// TODO Handle this
				}
			}
		}
	}

	@Override
	public void run() {

		solve();

	}

	private static synchronized void setValuesInCRD(ColoringReturnData crd, ColoringReturnData other_crd) {
		crd.setColors(other_crd.getColors());
		crd.status = other_crd.status;
	}

	// Get the degree of a vertex with adjacency matrix and index for vertex

	// get degree of vertex with the array already provided
	private static int getDegree(boolean[] vertex) {
		int sum = 0;
		for (boolean i : vertex)
			sum = (i) ? ++sum : sum;
		return sum;
	}

	private static int[] getDegreeArray(boolean[][] matrix) {
		int[] degrees = new int[matrix.length];
		for (int i = 0; i < matrix.length; i++) {
			degrees[i] = getDegree(matrix[i]);
		}
		return degrees;
	}

}
