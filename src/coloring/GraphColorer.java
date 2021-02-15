package coloring;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.Scanner;

import representations.GraphMatrixGenerator;

public class GraphColorer implements Runnable {

	private static volatile boolean flag = false;

	private int[][] adj_matrix;
	private int[] coloring;
	private int[][] domain;
	private int[] order;
	int max_colors;
	private ColoringReturnData top_cdr;

	private static int maxDegree(int[][] matrix) {
		int maxDegree = 0;
		for (int[] vertex : matrix) {
			int degree = 0;
			for (int i : vertex)
				degree += i;
			if (degree > maxDegree)
				maxDegree = degree;
		}
		return maxDegree;
	}

	private static class ColoringReturnData {

		int successful;
		int[] colors;
		int num_colors;

		ColoringReturnData(int successful, int[] colors) {
			this.successful = successful;
			this.colors = colors;
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
		void updateNumColors() {
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

	}

	public static void main(String[] args) {
		int[][] graphmatrix = null;

		if (args.length == 0) {
			graphmatrix = GraphMatrixGenerator.genUndirectedGraph(1000, 0.1f);
			GraphMatrixGenerator.printMatrix(graphmatrix);
		} else {
			String path = args[0];
			try {
				Scanner sc = new Scanner(new FileInputStream(new File(path)));
				String[] firstline = sc.nextLine().split(" ");

				int nodes = Integer.parseInt(firstline[0]);
				int edges = Integer.parseInt(firstline[1]);

				graphmatrix = new int[nodes][nodes];

				for (int i = 0; i < edges; i++) {
					String[] line = sc.nextLine().split(" ");
					graphmatrix[Integer.parseInt(line[0])][Integer.parseInt(line[1])] = 1;
					graphmatrix[Integer.parseInt(line[1])][Integer.parseInt(line[0])] = 1;
				}

				sc.close();

			} catch (FileNotFoundException e) {
				System.exit(0);
				e.printStackTrace();
			}
		}

		ColoringReturnData crd = new ColoringReturnData(0, null);
		GraphColorer the_colorer = new GraphColorer(graphmatrix, crd);

		Thread thread = null;

		int colors = maxDegree(graphmatrix);

		// if (args.length > 0) {
		// if (graphmatrix.length == 20)
		// colors = 7;
		// if (graphmatrix.length == 70)
		// colors = 21; // one of these
		// if (graphmatrix.length == 50)
		// colors = 21; //
		// if (graphmatrix.length == 100)
		// colors = 17;
		// if (graphmatrix.length == 250)
		// colors = 96;
		// if (graphmatrix.length == 500)
		// colors = 17;
		// if (graphmatrix.length == 1000)
		// colors = 125;
		// }

		int[] best_solution = null;
		while (the_colorer.top_cdr.successful == 0) {
			if (the_colorer.top_cdr.num_colors != 0)
				colors = the_colorer.top_cdr.num_colors - 1;
			else colors--;
			the_colorer.max_colors = colors;
			the_colorer.top_cdr.successful = 1;
			thread = new Thread(the_colorer, Integer.toHexString(Integer.toHexString(colors).hashCode()));
			thread.start();
			long t0 = System.currentTimeMillis();
			while (thread.isAlive() & System.currentTimeMillis() < t0 + 1200 * 1000) {

			}
			if (args.length != 1) {
				System.out.println("Tried with " + colors);
			}
			the_colorer.top_cdr.updateNumColors();

		}
		flag = true;
		while (thread.isAlive()) {

		}
		colors++;
		int[] final_output = the_colorer.colorGraphWithLimit(colors).colors;
		int maxColor = 0;
		for (int i = 0; i < final_output.length; i++) {
			if (maxColor < final_output[i])
				maxColor = final_output[i];
		}
		System.out.println((maxColor + 1) + " 0");

		for (int i = 0; i < final_output.length; i++) {

			System.out.print(final_output[i] + " ");
		}

		for (int i = 0; i < graphmatrix.length - 1; i++) {
			for (int j = i + 1; j < graphmatrix.length; j++) {
				if (final_output[i] == final_output[j] && graphmatrix[i][j] == 1) {
					System.out.print("\nCollision @ vertices " + i + ", " + j);
				}
			}
		}

	}

	public GraphColorer(int[][] graph, ColoringReturnData cdr) {

		adj_matrix = graph;
		coloring = new int[adj_matrix.length];
		for (int i = 0; i < adj_matrix.length; i++)
			coloring[i] = -1;
		domain = new int[adj_matrix.length][];

		int[] degree = new int[adj_matrix.length];
		for (int i = 0; i < degree.length; i++) {
			degree[i] = 0;
			for (int j : adj_matrix[i]) {
				degree[i] += j;
			}
		}
		order = new int[adj_matrix.length];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}

		top_cdr = cdr;

	}

	public GraphColorer(int[][] graph, int[] colors) {

		adj_matrix = graph;
		coloring = colors.clone();
		domain = new int[adj_matrix.length][];

		order = new int[adj_matrix.length];
		for (int i = 0; i < order.length; i++) {
			order[i] = i;
		}

	}

	public ColoringReturnData colorGraphWithLimit(int num_of_colors) {
		updateDomains(num_of_colors);

		for (int i = 0; i < adj_matrix.length; i++) {
			if (coloring[i] == -1) {
				if (domain[i].length == 0) {
					return new ColoringReturnData(1, null);
				}
				if (domain[i].length == 1) {
					coloring[i] = domain[i][0];
					for (int j = 0; j < adj_matrix.length; j++) {
						if (flag & !Thread.currentThread().getName().equals("main")) {
							return new ColoringReturnData(1, null);
						}
						if (adj_matrix[i][j] == 1) {
							updateDomainForVertex(num_of_colors, j);
						}
					}
				}
			}
		}

		int empty_index = -1;
		int empty_domain = num_of_colors + 1;
		int empty_degree = 0;
		for (int i = 0; i < adj_matrix.length; i++) {
			if (coloring[i] == -1 && (empty_domain > domain[i].length)
					| (empty_domain >= domain[i].length && empty_degree < getDegree(i))) {
				empty_index = i;
				empty_domain = domain[i].length;
				empty_degree = getDegree(i);
			}
		}

		if (empty_index == -1)
			return new ColoringReturnData(0, coloring);

		for (int color : domain[empty_index]) {
			if (flag & !Thread.currentThread().getName().equals("main")) {
				return new ColoringReturnData(1, null);
			}

			int[] next_coloring = coloring.clone();
			next_coloring[empty_index] = color;

			GraphColorer next_gc = new GraphColorer(adj_matrix, next_coloring);
			ColoringReturnData crd = next_gc.colorGraphWithLimit(num_of_colors);
			if (crd.successful == 0) {
				return crd;
			}
		}

		for (int i = 0; i < coloring.length; i++) {
			coloring[i] = -1;
		}
		return new ColoringReturnData(1, null);

	}

	public void colorGraphAtTopLevel(int num_of_colors) {
		updateDomains(num_of_colors);

		for (int i = 0; i < adj_matrix.length; i++) {
			if (coloring[i] == -1) {
				if (domain[i].length == 0) {
					return;
				}
				if (domain[i].length == 1) {
					coloring[i] = domain[i][0];
					for (int j = 0; j < adj_matrix.length; j++) {
						if (adj_matrix[i][j] == 1) {
							updateDomainForVertex(num_of_colors, j);
						}
					}
				}
			}
		}

		int empty_index = -1;
		int empty_domain = num_of_colors + 1;
		int empty_degree = 0;
		for (int i = 0; i < adj_matrix.length; i++) {
			if (coloring[i] == -1 && (empty_domain > domain[i].length)
					| (empty_domain >= domain[i].length && empty_degree < getDegree(i))) {
				empty_index = i;
				empty_domain = domain[i].length;
				empty_degree = getDegree(i);
			}
		}

		for (int color : domain[empty_index]) {

			if (flag & !Thread.currentThread().getName().equals("main")) {
				return;
			}

			int[] next_coloring = coloring.clone();
			next_coloring[empty_index] = color;

			GraphColorer next_gc = new GraphColorer(adj_matrix, next_coloring);
			ColoringReturnData crd = next_gc.colorGraphWithLimit(num_of_colors);
			if (crd.successful == 0) {
				int[] arranged_colors = new int[crd.colors.length];
				for (int i = 0; i < arranged_colors.length; i++) {
					arranged_colors[order[i]] = crd.colors[i];
				}
				crd.colors = arranged_colors;
				top_cdr.successful = crd.successful;
				top_cdr.colors = crd.colors;
				return;
			}
		}

		for (int i = 0; i < coloring.length; i++) {
			coloring[i] = -1;
		}

	}

	private void updateDomains(int num_of_colors) {
		for (int i = 0; i < coloring.length; i++) {
			if (coloring[i] == -1) {

				int[] color_candidates = colorCandidates(num_of_colors);
				int candidate_count = color_candidates.length;
				for (int j = 0; j < adj_matrix.length; j++) {
					if (adj_matrix[i][j] == 1 && i != j && coloring[j] != -1 && color_candidates[coloring[j]] != -1) {

						color_candidates[coloring[j]] = -1;
						candidate_count--;

					}
				}
				domain[i] = new int[candidate_count];
				int domain_index = 0;
				for (int j = 0; j < color_candidates.length; j++) {
					if (color_candidates[j] != -1) {
						domain[i][domain_index] = color_candidates[j];
						domain_index++;
					}
				}
			}
		}
	}

	private int[] colorCandidates(int num_of_colors) {
		int[] output = new int[num_of_colors];
		for (int i = 0; i < num_of_colors; i++) {
			output[i] = i;
		}
		return output;
	}

	private void updateDomainForVertex(int num_of_colors, int index) {
		if (coloring[index] == -1) {

			int[] color_candidates = colorCandidates(num_of_colors);
			int candidate_count = color_candidates.length;
			for (int j = 0; j < adj_matrix.length; j++) {
				if (adj_matrix[index][j] == 1 && index != j && coloring[j] != -1
						&& color_candidates[coloring[j]] != -1) {

					color_candidates[coloring[j]] = -1;
					candidate_count--;

				}
			}
			domain[index] = new int[candidate_count];
			int domain_index = 0;
			for (int j = 0; j < color_candidates.length; j++) {
				if (color_candidates[j] != -1) {
					domain[index][domain_index] = color_candidates[j];
					domain_index++;
				}
			}
		}
	}

	private int getDegree(int vertex) {
		int sum = 0;
		for (int i : adj_matrix[vertex]) {
			sum += i;
		}
		return sum;
	}

	@Override
	public void run() {
		colorGraphAtTopLevel(max_colors);

	}

}
