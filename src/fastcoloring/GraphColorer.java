package fastcoloring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Scanner;

import representations.GraphMatrixGenerator;

public class GraphColorer {

	static int max_colors;

	public static void main(String[] args) {

		// LOAD DATA

		graph = null;

		if (args.length == 0) {
			// Testing params
			int size = 10;
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

		max_colors = graph.MAX_DEGREE + 1; // Start off with max degree
											// colors

		int[] final_output = new int[graph.VERTICES];
		for (int i = 0; i < graph.VERTICES; i++) {
			final_output[i] = i;
		}
		while (current_crd.status == 0) {
			System.out.println(max_colors + "?");
			GraphColorer gc = new GraphColorer();
			current_crd = gc.solveFor(max_colors);
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

		// Submits solution and checks validity
		printSolution(final_output);

		System.exit(0);

	}

	private static void printSolution(int[] coloring) {
		int maxColor = 0;
		for (int i = 0; i < coloring.length; i++) {
			if (maxColor < coloring[i])
				maxColor = coloring[i];
		}
		System.out.println((maxColor + 1) + " 0");

		for (int i = 0; i < coloring.length; i++) {

			System.out.print(coloring[i] + " ");
		}

		for (int i = 0; i < graph.VERTICES - 1; i++) {
			for (int j = i + 1; j < graph.VERTICES; j++) {
				if (coloring[i] == coloring[j] && graph.ADJ_MAT[i][j]) {
					System.out.print("\nCollision @ vertices " + i + ", " + j);
				}
			}
		}
	}

	Mod[] modstack;
	int ms_pointer;
	int[] num_mods;
	boolean[][] domains;
	int[] order;
	int[] coloring;
	int colors;
	int loop_pointer;

	public ColoringReturnData solveFor(int colors) {

		coloring = new int[graph.VERTICES];
		for (int i = 0; i < coloring.length; i++) {
			coloring[i] = -1;
		}

		this.colors = colors;
		modstack = new Mod[(colors + 1) * graph.VERTICES];
		ms_pointer = 0;

		num_mods = new int[graph.VERTICES];
		domains = new boolean[graph.VERTICES][colors];

		for (int i = 0; i < graph.VERTICES; i++) {
			if (graph.DEGREE[i] >= colors) {
				continue;
			}
			for (int j = graph.DEGREE[i]; j < colors; j++) {
				domains[i][j] = true;
			}
		}

		order = getDegreeOrder();
		updateCeilings();

		loop_pointer = 0;
		boolean finished = false;

		int[] vertex_guesses = new int[graph.VERTICES];

		LOOP: while (loop_pointer != -1 && !finished) {
			if (isValidState()) {
				// (color) * graph.VERTICES + (vertex)
				int v_t_col = loop_pointer;
				if (v_t_col >= graph.VERTICES) {
					finished = true;
					break LOOP;
				}
				while (coloring[order[v_t_col]] != -1) {
					v_t_col++;
					if (v_t_col >= graph.VERTICES) {
						finished = true;
						break LOOP;
					}
				}
				v_t_col = order[v_t_col];
				int c_t_col = 0;
				while (domains[v_t_col][c_t_col])
					c_t_col++;
				colorVertex(v_t_col, c_t_col);
				vertex_guesses[loop_pointer] = v_t_col;
				updateCeilings();
				while (handleDefinites())
					;
				loop_pointer++;
			} else {
				loop_pointer--;
				int prev_color = coloring[vertex_guesses[loop_pointer]];
				while (num_mods[loop_pointer] > 0) {
					popModStack();
				}
				loop_pointer--;
				if (loop_pointer == -1)
					break LOOP;
				domains[vertex_guesses[loop_pointer + 1]][prev_color] = true;
				pushModStack(new Mod(Mod.DOM, prev_color * graph.VERTICES + vertex_guesses[loop_pointer + 1]));
				loop_pointer++;
			}
		}
		ColoringReturnData crd = (finished) ? new ColoringReturnData(0, coloring) : new ColoringReturnData(1, null);
		return crd;

	}

	private boolean handleDefinites() {
		boolean changed = false;
		Vertex: for (int i = 0; i < graph.VERTICES; i++) {
			if (coloring[i] != -1)
				continue;
			int j = 0;
			while (j < colors && domains[i][j])
				j++;
			if (j == colors)
				break Vertex;
			int col = j;
			while (++j < colors) {
				if (!domains[i][j])
					continue Vertex;
			}
			colorVertex(i, col);
			changed = true;
		}

		return changed;
	}

	private void updateCeilings() {
		int highest_col = 0;
		int i = 0;
		while (i < graph.VERTICES && coloring[order[i]] != -1) {
			if (coloring[order[i]] > highest_col)
				highest_col = coloring[order[i]];
			i++;
		}
		for (; i < graph.VERTICES && highest_col < colors && highest_col < graph.DEGREE[order[i]]; i++) {
			for (int j = colors - 1; j > highest_col + 1; j--) {
				if (!domains[order[i]][j]) {
					domains[order[i]][j] = true;
					pushModStack(new Mod(Mod.DOM, j * graph.VERTICES + order[i]));
				}
			}
			highest_col++;
		}
	}

	private void colorVertex(int v, int col) {
		coloring[v] = col;
		pushModStack(new Mod(Mod.COLOR, v));
		for (int i = 0; i < graph.VERTICES; i++) {
			if (graph.ADJ_MAT[v][i] && !domains[i][col] && coloring[i] == -1) {
				domains[i][col] = true;
				pushModStack(new Mod(Mod.DOM, col * graph.VERTICES + i));
			}
		}
	}

	private void pushModStack(Mod m) {
		modstack[ms_pointer++] = m;
		num_mods[loop_pointer]++;
	}

	private void popModStack() {
		modstack[--ms_pointer].undo();
		modstack[ms_pointer] = null;
		num_mods[loop_pointer]--;
	}

	private boolean isValidState() {
		Vertex: for (int i = 0; i < graph.VERTICES; i++) {
			if (coloring[i] == -1) {
				for (int j = 0; j < colors; j++) {
					if (!domains[i][j])
						continue Vertex;
				}
				return false;
			}
		}
		return true;
	}

	private class Mod {

		int type;
		int value;

		static final int COLOR = 0;
		static final int DOM = 1;

		private Mod(int t, int v) {
			type = t;
			value = v;
		}

		void undo() {
			switch (type) {
			case COLOR:
				coloring[value] = -1;
				break;
			case DOM:
				domains[value % graph.VERTICES][value / graph.VERTICES] = false;
				break;
			default:
				break;
			}
		}

	}

	private int[] getDegreeOrder() {
		int[] order = new int[graph.VERTICES];
		for (int i = 0; i < graph.VERTICES; i++) {
			order[i] = i;
		}
		int[] degree = graph.DEGREE.clone();
		for (int i = 0; i < graph.VERTICES; i++) {
			int max_deg = 0;
			int max_ind = i;
			for (int j = i; j < graph.VERTICES; j++) {
				if (degree[j] > max_deg) {
					max_deg = degree[j];
					max_ind = j;
				}
			}
			degree[max_ind] = degree[i];
			int c = order[i];
			order[i] = order[max_ind];
			order[max_ind] = c;
		}

		return order;

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
		}

	}

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
