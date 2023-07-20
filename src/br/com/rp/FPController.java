package br.com.rp;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Vector;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import com.futronic.SDKHelper.FTR_PROGRESS;
import com.futronic.SDKHelper.FtrIdentifyRecord;
import com.futronic.SDKHelper.FtrIdentifyResult;
import com.futronic.SDKHelper.FutronicEnrollment;
import com.futronic.SDKHelper.FutronicException;
import com.futronic.SDKHelper.FutronicIdentification;
import com.futronic.SDKHelper.FutronicSdkBase;
import com.futronic.SDKHelper.FutronicVerification;
import com.futronic.SDKHelper.IEnrollmentCallBack;
import com.futronic.SDKHelper.IIdentificationCallBack;
import com.futronic.SDKHelper.IVerificationCallBack;
import com.futronic.SDKHelper.VersionCompatible;
import com.google.gson.Gson;

public class FPController implements IEnrollmentCallBack, IVerificationCallBack, IIdentificationCallBack {

	/**
	 * Construntor privado a fim de que n�o seja possivel a cria��o de novas
	 * instancias fora da classe
	 */
	private FPController() {
		currentList = new ArrayList<Integer>();
	};

	private static class SingletonHelper {
		private static final FPController INSTANCE = new FPController();
	}

	public static FPController getInstance() {
		return SingletonHelper.INSTANCE;
	}

	private String generalStatus; // status geral de comunica��o com o leitor
	private String currentMsg;
	private String errMsg;
	private String resultStatus; // resultado da identifica��o
	private int identifyUserId; // id do usu�rio identificador
	private boolean FFD; // Fake Finger Detector
	private int FAR; // False Accepting Ratio
	private boolean fastMode; // Modo R�pido:
	private boolean MIOT; // Multiple fingers In One Template
	private int numTemplates;
	private int enrollQuality; // Qualidade da digital salva no Enroll
	private String enrollMinutia; // minucia encriptada
	private int enrollSize; // tamanho da digital
	private FtrIdentifyRecord[] rgRecords;
	private ArrayList<Integer> currentList;
	private String processorStatus; // status do componente responsavel por
									// processar a ultima digital do identify

	private java.awt.image.BufferedImage currentBitmap;

	/**
	 * Contain reference for current operation object
	 */
	private FutronicSdkBase m_Operation;

	/**
	 * A database directory name.
	 */
	private String m_DbDir;

	/**
	 * The type of this parameter is depending from current operation. For
	 * enrollment operation this is DbRecord.
	 */
	private Object m_OperationObj;

	private Logger logger = Logger.getLogger("MyLog");
	private static FileHandler fh;

	/**
	 * INITIALIZE First method to be called to start communication
	 * 
	 * @param bFFD         Fake finger detector
	 * 
	 * @param far          indicates False Acception Ratio (1 ... 1000)
	 * 
	 * @param databaseDir  folder where the minutias will be saved
	 * 
	 * @param bFastMode    activates fast Mode :identification process is speeding
	 *                     up 2-3 times by slightly increasing the FRR at the same
	 *                     FAR level
	 * @param bMIOT        activates Multiple fingers IN ONE Template
	 * @param numTemplates number of templates for each enroll
	 */
	public boolean initialize(boolean bFFD, int far, String databaseDir, boolean bFastMode, boolean bMIOT,
			int numTemplates) {

		try {

			System.out.println("RP - Biometry Authentication: Version 1.1 Build 221213.01");

			this.FFD = bFFD;
			this.FAR = far;
			this.fastMode = bFastMode;
			this.MIOT = bMIOT;
			this.numTemplates = numTemplates;

			// criando pasta log
			createStruture(databaseDir);

			try {
				m_Operation = new FutronicEnrollment();
				printInitValues();

			} catch (FutronicException e) {
				errMsg = "Exce��o em Initialize: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
				generalStatus = "ERROR";
				System.out.println("RP - Inicializa��o: Exce��o disparada.");
				insertLog(errMsg);
				return false;
			}
			m_Operation = null;

			generalStatus = "READY";
			System.out.println("Leitor Biom�trico - Inicializa��o: Dados recebidos com sucesso.");
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			errMsg = "Exce��o em Initialize: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
			generalStatus = "ERROR";
			System.out.println("RP - Inicializa��o: Exce��o disparada.");
			insertLog(errMsg);
			return false;
		}

	}

	public void printInitValues() {
		System.out.println("Leitor Biom�trico - Configura��es:\n" + "Detector Dedo Falso (FFD): " + this.FFD + "\n"
				+ "False Accepting Ratio (FAR): " + this.FAR + "\n" + "Diret�rio: " + this.m_DbDir + "\n"
				+ "Modo R�pido (FastMode): " + this.fastMode + "\n" + "V�rios dedos em um template (MIOT): " + this.MIOT
				+ "\n" + "Digitais por Template: " + this.numTemplates + "\n");

	}

	public String getGeneralStatus() {
		return this.generalStatus;
	}

	public int getEnrollQuality() {
		return this.enrollQuality;
	}

	public String getCurrentMsg() {
		return this.currentMsg;
	}

	/*
	 * ACTIVATE FAKE FINGER DETECTOR
	 */
	public void setFFD(boolean fFD) {
		this.FFD = fFD;
	}

	/*
	 * define valor de 1 a 1000 para FARLEVEL
	 */
	public void setFAR(int far) {
		this.FAR = far;
	}

	/*
	 * define valor para Multiple Fingers In One Template
	 */
	public void setMIOT(boolean miot) {
		this.MIOT = miot;
	}

	/*
	 * define numero de templates a serem cadastrados por usu�rio no Enroll
	 */
	public void setNumTemplates(int n) {
		this.numTemplates = n;
	}

	/*
	 * Ativa/desativa FastMode With this value set to TRUE the identification
	 * process is speeding up 2-3 times by slightly increasing the FRR at the same
	 * FAR level
	 */
	public void setFastMode(boolean fastMode) {
		this.fastMode = fastMode;
	}

	////////////////////////////////////////////////////////////////////
	// ICallBack interface implementation
	////////////////////////////////////////////////////////////////////

	/**
	 * The "Put your finger on the scanner" event.
	 *
	 * @param Progress the current progress data structure.
	 */
	public void OnPutOn(FTR_PROGRESS Progress) {
		this.currentMsg = "Posicione o dedo sobre o leitor biom�trico";
		System.out.println(this.currentMsg);

	}

	/**
	 * The "Take off your finger from the scanner" event.
	 *
	 * @param Progress the current progress data structure.
	 */
	public void OnTakeOff(FTR_PROGRESS Progress) {
		this.currentMsg = "Retire o dedo do leitor";
		System.out.println(this.currentMsg);
	}

	/**
	 * The "Show the current fingerprint image" event.
	 *
	 * @param Bitmap the instance of Bitmap class with fingerprint image.
	 */
	public void UpdateScreenImage(java.awt.image.BufferedImage Bitmap) {
		this.currentBitmap = Bitmap;
	}

	/**
	 * The "Fake finger detected" event.
	 *
	 * @param Progress the fingerprint image.
	 *
	 * @return <code>true</code> if the current indetntification operation should be
	 *         aborted, otherwise is <code>false</code>
	 */
	public boolean OnFakeSource(FTR_PROGRESS Progress) {
		/*
		 * int nResponse; nResponse = JOptionPane.showConfirmDialog( this,
		 * "Fake source detected. Do you want continue process?", getTitle(),
		 * JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE ); return (nResponse
		 * == JOptionPane.NO_OPTION);
		 */
		this.currentMsg = "Dedo falso detectado.";
		System.out.println(this.currentMsg);
		return false;
	}

	////////////////////////////////////////////////////////////////////
	// ICallBack interface implementation
	////////////////////////////////////////////////////////////////////

	/**
	 * The "Enrollment operation complete" event.
	 *
	 * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
	 *                 <code>false</code>.
	 * @param The      Futronic SDK return code (see FTRAPI.h).
	 */
	public void OnEnrollmentComplete(boolean bSuccess, int nResult) {

		if (bSuccess) {

			try {

				byte[] template = ((FutronicEnrollment) m_Operation).getTemplate();

				// obtem qualidade da digital cadastrada
				this.enrollQuality = ((FutronicEnrollment) m_Operation).getQuality();

				// Set template into passport and save it
				((DbRecord) m_OperationObj).setTemplate(template);

				// Salvando arquivo da minucia
				if (((DbRecord) m_OperationObj)
						.Save(m_DbDir + File.separator + ((DbRecord) m_OperationObj).getStringUserId())) {

					// valorando variavel que registra a minucia encriptada
					this.enrollMinutia = Base64.getEncoder().encodeToString(template);
					this.enrollSize = template.length;

					System.out.println("Size: " + this.enrollSize);

					resultStatus = "S";
					System.out.println("Sucesso. Digital salva.");

				} else {
					resultStatus = "N";
					System.out.println("Falha. Digital N�O salva.");
				}

			} catch (IOException e) {
				resultStatus = "E";
				generalStatus = "READY";
				e.printStackTrace();
				errMsg = "Exce��o em OnEnrollmentComplete: "
						+ org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
			}

		} else {
			resultStatus = ".";
			// ignorar retornos 8(cancel) e 205(fake finger)
			if (nResult != 8 && nResult != 205) {
				resultStatus = "E";
				generalStatus = "ERROR";
				// inserindo erro no arquivo de log
				this.errMsg = "Processo Enroll falhou. Retorno " + nResult + " : "
						+ FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(this.errMsg);
			}

		}

		m_Operation = null;
		m_OperationObj = null;

		if (generalStatus != "ERROR") {
			generalStatus = "READY";
		}
	}

	/**
	 * The "Verification operation complete" event.
	 *
	 * @param bSuccess             <code>true</code> if the operation succeeds,
	 *                             otherwise is <code>false</code>
	 * @param nResult              the Futronic SDK return code.
	 * @param bVerificationSuccess if the operation succeeds (bSuccess is
	 *                             <code>true</code>), this parameters shows the
	 *                             verification operation result. <code>true</code>
	 *                             if the captured from the attached scanner
	 *                             template is matched, otherwise is
	 *                             <code>false</code>.
	 */
	public void OnVerificationComplete(boolean bSuccess, int nResult, boolean bVerificationSuccess) {

		if (bSuccess) {
			if (bVerificationSuccess) {
				System.out.println("Successo. A digital � do usu�rio informado.");
				resultStatus = "S";
			} else {
				System.out.println("Falha. A digital n�o � do usu�rio informado.");
				resultStatus = "N";
			}

		} else {
			resultStatus = ".";
			// ignorar retornos 8(cancel) e 205(fake finger)
			if (nResult != 8 && nResult != 205) {
				resultStatus = "E";
				generalStatus = "ERROR";
				errMsg = "Processo Verify falhou. Retorno: " + nResult + " : "
						+ FutronicSdkBase.SdkRetCode2Message(nResult);
				System.out.println(errMsg);
				insertLog(errMsg);
			}
		}

		if (generalStatus != "ERROR") {
			generalStatus = "READY";
		}

	}
	//
	// public int isAlreadyFound(byte[] baseTemplate) {
	//
	// int isAlreadyFound = 0;
	// int nResult;
	//
	// if (!prepareList()) {
	//
	// ((FutronicIdentification) m_Operation).setBaseTemplate(baseTemplate);
	//
	// System.out.println("Starting identification...");
	//
	// FtrIdentifyResult result = new FtrIdentifyResult();
	//
	// nResult = ((FutronicIdentification)
	// m_Operation).Identification(rgRecords, result);
	// if (nResult == FutronicSdkBase.RETCODE_OK) {
	//
	// if (result.m_Index != -1) {
	//
	// isAlreadyFound = 1;
	// identifyUserId = currentList.get(result.m_Index);
	//
	// System.out.println("Usu�rio j� est� cadastrado. " + identifyUserId);
	//
	// } else {
	// isAlreadyFound = 2;
	// resultStatus = "N"; // not found
	// System.out.println("Falha. Usu�rio n�o identificado.");
	// }
	//
	// } else {
	// isAlreadyFound = 2;
	// }
	//
	// }
	//
	// return isAlreadyFound;
	// }

	/**
	 * The "Get base template operation complete" event.
	 *
	 * @param bSuccess <code>true</code> if the operation succeeds, otherwise is
	 *                 <code>false</code>.
	 * @param nResult  The Futronic SDK return code.
	 */
	public void OnGetBaseTemplateComplete(boolean bSuccess, int nResult) {

		// variavel que indica se a digital caputurada deve ser processada
		boolean processFP = false;

		//
		if (bSuccess) {

			System.out.println("Starting identification...");

			FtrIdentifyResult result = new FtrIdentifyResult();

			nResult = ((FutronicIdentification) m_Operation).Identification(rgRecords, result);
			if (nResult == FutronicSdkBase.RETCODE_OK) {

				if (result.m_Index != -1) {

					identifyUserId = currentList.get(result.m_Index);
					resultStatus = "S"; // success
					System.out.println("Sucesso: User: " + identifyUserId);

				} else {

					resultStatus = "N"; // not found
					System.out.println("Falha. Usu�rio n�o identificado.");
				}

				processFP = true;

			} else {
				resultStatus = "E"; // error
				errMsg = "Identification failed: " + nResult + " - " + FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(errMsg);
			}

		} else {

			switch (nResult) {
			case 205: // 205 = fake finger, � disparado quando o
				// usuario n�o coloca o dedo inteiro/ leitor
				// sujo/dedo falso
				resultStatus = "205";
				processFP = true;
				break;

			case 8: // 8 = identify cancelado (causado pela chamada
					// do cancel)
				// do nothing
				resultStatus = "8";
				break;

			case 6:
				processFP = true;
				resultStatus = "6"; // error
				generalStatus = "ERROR";
				errMsg = "Error description: " + nResult + " - " + FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(errMsg);
				break;
			case 9:
				processFP = true;
				resultStatus = "9"; // error
				generalStatus = "ERROR";
				errMsg = "Error description: " + nResult + " - " + FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(errMsg);
				break;
			default: // registrar demais erros

				resultStatus = "E"; // error
				generalStatus = "ERROR";
				errMsg = "Error description: " + nResult + " - " + FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(errMsg);
				break;
			}

		}

		m_Operation = null;
		m_OperationObj = null;

		if (generalStatus != "ERROR") {
			generalStatus = "READY";
		}

		// processar digital?
		if (processFP) {
			// CLICANDO no componente da tela responsavel por processar a
			// digital do usuario
			mouseClick(1022, 763);
		}

	}

	public void insertLog(String msg) {
		try {
			// the following statement is used to log any messages
			logger.info(msg);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * EXCLUI TODAS MINUCIAS CADASTRADAS NO EQUIPAMENTO
	 */
	public void deleteAll() {

		try {

			File dir = new File(m_DbDir);

			for (File f : dir.listFiles()) {

				// n�o excluir o log
				if (f.getName() != "DKFingerPrint.log") {
					f.delete();
					// System.out.println("Digital ID: "+f.getName()+" exclu�da
					// com sucesso.");
				}
			}

			System.out.println("Todas digitais exclu�das com sucesso.");

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void createStruture(String databaseDir) {

		try {

			m_DbDir = databaseDir;
			File dir = new File(databaseDir);
			boolean b = true;

			if (!dir.exists()) {

				b = dir.mkdirs();
				if (b)
					System.out.println("Directory successfully created");
				else
					System.out.println("Create Directory Failure");
			} else {
				System.out.println("Directory already created");
			}

			if (b == true) {

				// fh = new FileHandler("FPLogs/DKFingerPrint.log", true);
				fh = new FileHandler(databaseDir + "/DKFingerPrint.log", true);
				logger.addHandler(fh);
				SimpleFormatter formatter = new SimpleFormatter();
				fh.setFormatter(formatter);
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public boolean prepareList() {

		this.rgRecords = null;
		this.currentList.clear();

		try {

			// obtendo minucias presentes no diretorio configurado
			Vector<DbRecord> Users = DbRecord.ReadRecords(m_DbDir);

			// lista vazia?
			if (Users.size() == 0) {
				this.resultStatus = "0"; // zero minutias
				this.errMsg = "N�o h� usu�rios cadastrados para iniciar uma Identifica��o.";
				insertLog(errMsg);
				return false;

			}

			rgRecords = new FtrIdentifyRecord[Users.size()];

			// criando lista de minucias e arrays de IDs x Index
			for (int iUsers = 0; iUsers < Users.size(); iUsers++) {
				rgRecords[iUsers] = Users.get(iUsers).getFtrIdentifyRecord();
				this.currentList.add(iUsers, Users.get(iUsers).getIntUserId());

			}

			return true;
		} catch (Exception e) {
			e.printStackTrace();
			errMsg = "Exce��o em prepareList " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

			insertLog(errMsg);
			return false;
		}
	}

	/**
	 * EXECUTAR PROCESSO DE IDENTIFICA��O DE DIGITAL
	 * 
	 * @return status da execu��o do identify
	 */
	public boolean identify() {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma opera��o n�o possa ser chamada ao mesmo tempo)
//		if (generalStatus == "READY") {

		generalStatus = "IDENTIFY";
		identifyUserId = 0;

		// preparar lista de minucias para o Identify
		if (!prepareList()) {
			resultStatus = "E";
			generalStatus = "READY";
			return false;
		} else {

			this.resultStatus = "W"; // waiting...

			try {

				m_Operation = new FutronicIdentification();

				// Set control properties
				m_Operation.setFakeDetection(this.FFD);
				m_Operation.setFFDControl(this.FFD);
				m_Operation.setFARN(this.FAR);
				m_Operation.setVersion(VersionCompatible.ftr_version_current);
				m_Operation.setFastMode(this.fastMode);

				System.out.println("Identify iniciado");
				// start verification process
				((FutronicIdentification) m_Operation).GetBaseTemplate(this);

				return true;

			} catch (FutronicException e) {
				e.printStackTrace();
				resultStatus = "E";
				errMsg = "Exce��o em identify: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				return false;
			}
		}

//		} else {
//			this.errMsg = "Identify n�o executado pois j� existe outra opera��o em execu��o: \n" + generalStatus;
//			insertLog(this.errMsg);
//			return false;
//		}
	}

	public String getResultStatus() {
		return this.resultStatus;
	}

	/**
	 * CANCEL CURRENT OPERATION
	 */
	public void cancel() {
		m_Operation.OnCalcel();
		System.out.println("Opera��o Cancelada");
		generalStatus = "READY";
	}

	/**
	 * EXECUTAR PROCESSO DE VERIFICA��O DA DIGITAL
	 * 
	 * @param userId Id do usu�rio para ser utilizado no processo de verifica��o
	 * @return status da execu��o do verify
	 */
	public boolean verify(int userId) {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma opera��o n�o possa ser chamada ao mesmo tempo)
		if (generalStatus == "READY") {

			resultStatus = "W";
			generalStatus = "VERIFY";

			// obtendo lista de digitais cadastradas no sistema
			Vector<DbRecord> Users = DbRecord.ReadRecords(m_DbDir);

			// lista vazia?
			if (Users.size() == 0) {
				generalStatus = "READY";
				this.resultStatus = "0"; // zero minutias
				this.errMsg = "N�o h� usu�rios cadastrados para iniciar a Verifica��o.";
				insertLog(errMsg);
				return false;
			}

			DbRecord SelectedUser = null;

			for (DbRecord user : Users) {
				if (user.getIntUserId() == userId) {
					SelectedUser = user;
					break;
				}
			}

			// Usu�rio n�o encontrado?
			if (SelectedUser == null) {
				generalStatus = "READY";
				this.resultStatus = "0"; // zero minutias
				this.errMsg = "Usu�rio ID: " + userId + " n�o encontrado na base de digitais cadastradas. ";
				insertLog(errMsg);
				return false;
			}

			m_OperationObj = SelectedUser;

			try {
				m_Operation = new FutronicVerification(SelectedUser.getTemplate());

				// Set control properties
				m_Operation.setFakeDetection(this.FFD);
				m_Operation.setFFDControl(this.FFD);
				m_Operation.setFARN(this.FAR);
				m_Operation.setVersion(VersionCompatible.ftr_version_current);
				m_Operation.setFastMode(this.fastMode);

				System.out.println("Verify iniciado");
				// start verification process
				((FutronicVerification) m_Operation).Verification(this);

				return true;

			} catch (FutronicException e) {
				resultStatus = "E";
				e.printStackTrace();
				errMsg = "Exce��o em verify: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				return false;
			}

		} else {

			insertLog("Verify n�o executado pois j� existe outra opera��o em execu��o: \n" + generalStatus);
			return false;
		}
	}

	public String getErrMsg() {
		return this.errMsg;
	}

	/**
	 * EXECUTAR PROCESSO DE CADASTRO DA DIGITAL
	 * 
	 * @param userId Id do usu�rio que ter� sua digital cadastrada
	 * @return status da execu��o do enroll
	 */
	public boolean enroll(int userId) {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma opera��o n�o possa ser chamada ao mesmo tempo)
		if (generalStatus == "READY") {

			// valorando variavel que recebe o template da digital como nulo
			this.enrollMinutia = null;
			this.enrollQuality = 0;
			resultStatus = "W"; // waiting...
			generalStatus = "ENROLL";

			try {

				if (userId == 0) {
					generalStatus = "READY";
					this.errMsg = "Usu�rio ID: " + userId + " inv�lido.";
					insertLog(errMsg);
					return false;
				}

				String fileName = String.valueOf(userId);

				// se arquivo n�o exitir...
				if (!fileExists(fileName)) {
					// fa�a um teste de cria��o do arquivo
					CreateFile(fileName);
				}

				m_OperationObj = new DbRecord();
				((DbRecord) m_OperationObj).setUserId(fileName);
				;

				m_Operation = new FutronicEnrollment();

				// Set control properties
				m_Operation.setFakeDetection(this.FFD);
				m_Operation.setFFDControl(this.FFD);
				m_Operation.setFARN(this.FAR);
				m_Operation.setVersion(VersionCompatible.ftr_version_current);
				m_Operation.setFastMode(this.fastMode);

				((FutronicEnrollment) m_Operation).setMIOTControlOff(!this.MIOT);
				((FutronicEnrollment) m_Operation).setMaxModels(this.numTemplates);

				System.out.println("Enrollment iniciado");
				// start enrollment process
				((FutronicEnrollment) m_Operation).Enrollment(this);

				return true;

			} catch (Exception e) {
				resultStatus = "E";
				e.printStackTrace();
				errMsg = "Exce��o em enroll: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				generalStatus = "READY";
				return false;
			}

		} else {
			errMsg = "Enroll n�o executado pois j� existe outra opera��o em execu��o: \n" + generalStatus;
			insertLog(errMsg);
			return false;
		}

	}

	private boolean fileExists(String fileName) {
		File f = new File(m_DbDir, fileName);
		return f.exists();
	}

	private void CreateFile(String szFileName) throws AppException {
		File f = new File(m_DbDir, szFileName);
		try {
			f.createNewFile();
			f.delete();
		} catch (IOException e) {
			throw new AppException("Can not create file " + szFileName + " in database.");
		} catch (SecurityException e) {
			throw new AppException("Can not create file " + szFileName + " in database. Access denied");
		}
	}

	public void mouseClick(int x, int y) {

		// indicando que digital ainda n�o foi processada pela APP do
		// Dispens�rio
		processorStatus = "N";
		int tries = 0;

		try {

			java.awt.Robot robot = new java.awt.Robot();

			// enquanto APP n�o processar a digital CLIQUE no componente
			// processador por 5 tentantivas
			// a APP do dispens�rio ir� valorar processorStatus = "S" quando o
			// componente for clicado
			do {

				robot.mouseMove(x, y);
				robot.mousePress(java.awt.event.InputEvent.BUTTON1_MASK);
				robot.mouseRelease(java.awt.event.InputEvent.BUTTON1_MASK);
				System.out.println("Clique Executado.");

				// aguarde 2 segundos
				Thread.sleep(2000);
				tries += 1;

			} while (processorStatus == "N" && tries < 3);

			// chamar novo identify se a digital do usu�rio anterior n�o foi
			// processada
			if (processorStatus == "N" && tries == 3) {
				if (generalStatus == "READY")
					identify();
			}

		} catch (Exception e) {
			e.printStackTrace();
			errMsg = org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
			insertLog(errMsg);
		}

	}

	/**
	 * UPDATEUSERLIST Receber a lista de digitais cadastradas e salvar na instancia
	 * local
	 * 
	 * @param newUserList LIsta de digitais dos usu�rios com seus respectivos ids
	 * 
	 * @return status indicador de sucesso/falha de execu��o
	 * 
	 */
	public boolean updateUserList(String userListJSON) {

		generalStatus = "UPDATING";

		boolean status = false;

		// transformando userListJSON em um array de DankiaFPTemplate
		Gson gson = new Gson();
		DankiaFPTemplate newUserList[] = gson.fromJson(userListJSON, DankiaFPTemplate[].class);

		// lista n�o vazia e n�o nula?
		if (newUserList.length > 0 && newUserList != null) {
			status = true;

			String strUserId = "";

			// percorrendo lista de usuarios
			for (DankiaFPTemplate fp : newUserList) {

				strUserId = String.valueOf(fp.getUserId());

				// insira a minucia no servi�o da biometria
				addMinutia(strUserId, fp.getStrMinutiae());

			}

		} else {
			errMsg = "UpdateUserList: Lista vazia/nula";
			insertLog(errMsg);
			status = false;
		}

		generalStatus = "READY";
		return status;
	}

	public boolean addMinutia(String strUserId, String base64) {

		try {

			// convertendo string para array de bytes
			byte[] templateByteArray = getBase64(base64);

			if (templateByteArray.length > 0) {

				if (templateByteArray.length == 669 || templateByteArray.length == 1335
						|| templateByteArray.length == 2001 || templateByteArray.length == 2667
						|| templateByteArray.length == 3333) {

					// criando nova minucia
					DbRecord minutia = new DbRecord();
					minutia.setUserId(strUserId);
					minutia.setTemplate(templateByteArray);

					// Salvando arquivo da minucia
					if (minutia.Save(m_DbDir + File.separator + strUserId) == true) {

						//System.out.println(
							//	"Digital ID " + strUserId + " salva com sucesso. Size: " + templateByteArray.length);
						return true;

					} else {
						errMsg = "AddMinutia: .Save() retornou false. Digital n�o foi adicionada.";
						insertLog(errMsg);
						return false;
					}
				} else {

					errMsg = "AddMinutia: Base64 de tamanho inv�lido:" + templateByteArray.length + ". UserId: "
							+ strUserId;
					insertLog(errMsg);
					return false;

				}

			} else {
				errMsg = "AddMinutia: Base64 n�o decodificado. Verifique o campo UserFingerPrintBase64 do UserId: "
						+ strUserId;
				insertLog(errMsg);
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			errMsg = "Exce��o em addMinutia: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
			generalStatus = "READY";
			insertLog(errMsg);
			return false;

		}

	}

	/**
	 * METODO PARA VERIFICAR SE A STRING RECEBIDA EST� ENCRIPTADA EM BASE64
	 * 
	 * @param str
	 * @param userId
	 * @return
	 */
	public byte[] getBase64(String str) {

		byte[] auxByteArray = new byte[0];

		try {

			auxByteArray = Base64.getDecoder().decode(str);

			return auxByteArray;
		} catch (IllegalArgumentException e) {

			return new byte[0];

		}

	}

	/**
	 * Obtem a ultima digital cadastrada
	 * 
	 * @return ultima minucia cadastrada atrav�s da opera��o Enroll
	 */
	public String getEnrollMinutia() {
		return this.enrollMinutia;
	}

	/**
	 * OBTEM o �ltimo usu�rio identificado no Identify
	 * 
	 * @return Id do usu�rio identificado
	 */
	public int getIdentifyUserId() {
		return this.identifyUserId;
	}

	public String getProcessorStatus() {
		return processorStatus;
	}

	public void setProcessorStatus(String processorStatus) {
		this.processorStatus = processorStatus;
	}

	public void terminate() {

		if (m_Operation != null) {

			m_Operation.Dispose();
			generalStatus = "SHUT_DOWN";
		}

		System.out.println("Terminate executado");
	}

	public int getEnrollSize() {
		return enrollSize;
	}

}
