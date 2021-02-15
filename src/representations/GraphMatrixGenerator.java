package representations;

import java.util.Random;
import java.util.Scanner;

public class GraphMatrixGenerator {

	public static void main(String[] args) {
		Scanner sc = new Scanner(System.in);
		int v = sc.nextInt();
		float c = sc.nextFloat();

		int[][] undirected_connection_matrix = genUndirectedGraph(v, c);
		int[][] directed_connection_matrix = genDirectedGraph(v, c);

		for (int i = 0; i < v; i++) {
			System.out.print("\n");
			for (int j = 0; j < v; j++) {
				System.out.print(undirected_connection_matrix[i][j] + " ");
			}
		}
		System.out.println("\n");

		sc.close();
	}

	public GraphMatrixGenerator() {

	}

	public static int[][] genUndirectedGraph(int vertices, float connection) {
		Random random = new Random();
		int[][] output = new int[vertices][vertices];
		for (int i = 0; i < vertices; i++) {
			for (int j = 0; j <= i; j++) {
				if (j == i) {
					output[i][j] = 0;
				} else {
					if (random.nextFloat() < connection) {
						output[i][j] = 1;
						output[j][i] = 1;
					} else {
						output[i][j] = 0;
						output[j][i] = 0;
					}
				}
			}
		}
		return output;
	}

	public static int[][] genDirectedGraph(int vertices, float connection) {
		Random random = new Random();
		int[][] output = new int[vertices][vertices];
		for (int i = 0; i < vertices; i++) {
			for (int j = 0; j < vertices; j++) {
				if (j == i) {
					output[i][j] = 0;
				} else {
					if (random.nextFloat() < connection) {
						output[i][j] = 1;
					} else {
						output[i][j] = 0;
					}
				}
			}
		}
		return output;
	}

	public static void printMatrix(int[][] matrix) {
		for (int i = 0; i < matrix.length; i++) {
			System.out.print("\n");
			for (int j = 0; j < matrix.length; j++) {
				System.out.print(matrix[i][j] + " ");
			}
		}
		System.out.println("\n");
	}

	public static int maxDegree(int[][] matrix) {
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

}