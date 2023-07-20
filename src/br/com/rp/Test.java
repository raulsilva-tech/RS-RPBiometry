package br.com.rp;

import java.io.File;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub

		FPController controller = FPController.getInstance();

		try {

			String dir = "c:" + File.separator + "applications" + File.separator + "biometria";

			controller.initialize(true, 345, dir, false, true, 3);

			boolean keepGoing = true;

			String status;

			// TESTE IDENTIFY
			do {

				 Thread.sleep(2000);

				//chamando identify
				if (controller.identify() == true) {

					//obtendo status 
					status = controller.getGeneralStatus();

					do {

						//guarde 250ms
						Thread.sleep(250);

						//obtendo status novamente
						status = controller.getGeneralStatus();

					} while (status == "IDENTIFY");
					
							
					if (status == "ERROR") {

						//keepGoing = false;

					}

				}

			} while (keepGoing == true);

			controller.terminate();

			//
			// do {
			//
			// Thread.sleep(250);
			// // TESTE ENROLL
			// controller.enroll(4);
			//
			// while (controller.getResultStatus() == "W") {
			//
			// Thread.sleep(250);
			//
			// }
			//
			// System.out.println("Minucia: " + controller.getEnrollMinutia());
			//
			// } while (true);

			// TESTE ADDMINUTIA
			// String base64 =
			// "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";
			//
			// if (controller.addMinutia("4", base64))
			// System.out.println("Sucesso");
			// else
			// System.out.println("Falha");

		} catch (Exception e) {
			e.printStackTrace();
			controller.terminate();
		}

	}

}
