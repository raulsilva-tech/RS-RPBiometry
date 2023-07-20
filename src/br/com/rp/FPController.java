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
	 * Construntor privado a fim de que não seja possivel a criação de novas
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

	private String generalStatus; // status geral de comunicação com o leitor
	private String currentMsg;
	private String errMsg;
	private String resultStatus; // resultado da identificação
	private int identifyUserId; // id do usuário identificador
	private boolean FFD; // Fake Finger Detector
	private int FAR; // False Accepting Ratio
	private boolean fastMode; // Modo Rápido:
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
				errMsg = "Exceção em Initialize: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
				generalStatus = "ERROR";
				System.out.println("RP - Inicialização: Exceção disparada.");
				insertLog(errMsg);
				return false;
			}
			m_Operation = null;

			generalStatus = "READY";
			System.out.println("Leitor Biométrico - Inicialização: Dados recebidos com sucesso.");
			return true;

		} catch (Exception e) {
			e.printStackTrace();
			errMsg = "Exceção em Initialize: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
			generalStatus = "ERROR";
			System.out.println("RP - Inicialização: Exceção disparada.");
			insertLog(errMsg);
			return false;
		}

	}

	public void printInitValues() {
		System.out.println("Leitor Biométrico - Configurações:\n" + "Detector Dedo Falso (FFD): " + this.FFD + "\n"
				+ "False Accepting Ratio (FAR): " + this.FAR + "\n" + "Diretório: " + this.m_DbDir + "\n"
				+ "Modo Rápido (FastMode): " + this.fastMode + "\n" + "Vários dedos em um template (MIOT): " + this.MIOT
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
	 * define numero de templates a serem cadastrados por usuário no Enroll
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
		this.currentMsg = "Posicione o dedo sobre o leitor biométrico";
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
					System.out.println("Falha. Digital NÃO salva.");
				}

			} catch (IOException e) {
				resultStatus = "E";
				generalStatus = "READY";
				e.printStackTrace();
				errMsg = "Exceção em OnEnrollmentComplete: "
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
				System.out.println("Successo. A digital é do usuário informado.");
				resultStatus = "S";
			} else {
				System.out.println("Falha. A digital não é do usuário informado.");
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
	// System.out.println("Usuário já está cadastrado. " + identifyUserId);
	//
	// } else {
	// isAlreadyFound = 2;
	// resultStatus = "N"; // not found
	// System.out.println("Falha. Usuário não identificado.");
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
					System.out.println("Falha. Usuário não identificado.");
				}

				processFP = true;

			} else {
				resultStatus = "E"; // error
				errMsg = "Identification failed: " + nResult + " - " + FutronicSdkBase.SdkRetCode2Message(nResult);
				insertLog(errMsg);
			}

		} else {

			switch (nResult) {
			case 205: // 205 = fake finger, é disparado quando o
				// usuario não coloca o dedo inteiro/ leitor
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

				// não excluir o log
				if (f.getName() != "DKFingerPrint.log") {
					f.delete();
					// System.out.println("Digital ID: "+f.getName()+" excluída
					// com sucesso.");
				}
			}

			System.out.println("Todas digitais excluídas com sucesso.");

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
				this.errMsg = "Não há usuários cadastrados para iniciar uma Identificação.";
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
			errMsg = "Exceção em prepareList " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

			insertLog(errMsg);
			return false;
		}
	}

	/**
	 * EXECUTAR PROCESSO DE IDENTIFICAÇÃO DE DIGITAL
	 * 
	 * @return status da execução do identify
	 */
	public boolean identify() {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma operação não possa ser chamada ao mesmo tempo)
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
				errMsg = "Exceção em identify: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				return false;
			}
		}

//		} else {
//			this.errMsg = "Identify não executado pois já existe outra operação em execução: \n" + generalStatus;
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
		System.out.println("Operação Cancelada");
		generalStatus = "READY";
	}

	/**
	 * EXECUTAR PROCESSO DE VERIFICAÇÃO DA DIGITAL
	 * 
	 * @param userId Id do usuário para ser utilizado no processo de verificação
	 * @return status da execução do verify
	 */
	public boolean verify(int userId) {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma operação não possa ser chamada ao mesmo tempo)
		if (generalStatus == "READY") {

			resultStatus = "W";
			generalStatus = "VERIFY";

			// obtendo lista de digitais cadastradas no sistema
			Vector<DbRecord> Users = DbRecord.ReadRecords(m_DbDir);

			// lista vazia?
			if (Users.size() == 0) {
				generalStatus = "READY";
				this.resultStatus = "0"; // zero minutias
				this.errMsg = "Não há usuários cadastrados para iniciar a Verificação.";
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

			// Usuário não encontrado?
			if (SelectedUser == null) {
				generalStatus = "READY";
				this.resultStatus = "0"; // zero minutias
				this.errMsg = "Usuário ID: " + userId + " não encontrado na base de digitais cadastradas. ";
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
				errMsg = "Exceção em verify: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				return false;
			}

		} else {

			insertLog("Verify não executado pois já existe outra operação em execução: \n" + generalStatus);
			return false;
		}
	}

	public String getErrMsg() {
		return this.errMsg;
	}

	/**
	 * EXECUTAR PROCESSO DE CADASTRO DA DIGITAL
	 * 
	 * @param userId Id do usuário que terá sua digital cadastrada
	 * @return status da execução do enroll
	 */
	public boolean enroll(int userId) {

		// somente executar identify se status = pronto ( a fim de que mais de
		// uma operação não possa ser chamada ao mesmo tempo)
		if (generalStatus == "READY") {

			// valorando variavel que recebe o template da digital como nulo
			this.enrollMinutia = null;
			this.enrollQuality = 0;
			resultStatus = "W"; // waiting...
			generalStatus = "ENROLL";

			try {

				if (userId == 0) {
					generalStatus = "READY";
					this.errMsg = "Usuário ID: " + userId + " inválido.";
					insertLog(errMsg);
					return false;
				}

				String fileName = String.valueOf(userId);

				// se arquivo não exitir...
				if (!fileExists(fileName)) {
					// faça um teste de criação do arquivo
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
				errMsg = "Exceção em enroll: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);

				insertLog(errMsg);
				m_Operation = null;
				m_OperationObj = null;
				generalStatus = "READY";
				return false;
			}

		} else {
			errMsg = "Enroll não executado pois já existe outra operação em execução: \n" + generalStatus;
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

		// indicando que digital ainda não foi processada pela APP do
		// Dispensário
		processorStatus = "N";
		int tries = 0;

		try {

			java.awt.Robot robot = new java.awt.Robot();

			// enquanto APP não processar a digital CLIQUE no componente
			// processador por 5 tentantivas
			// a APP do dispensário irá valorar processorStatus = "S" quando o
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

			// chamar novo identify se a digital do usuário anterior não foi
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
	 * @param newUserList LIsta de digitais dos usuários com seus respectivos ids
	 * 
	 * @return status indicador de sucesso/falha de execução
	 * 
	 */
	public boolean updateUserList(String userListJSON) {

		generalStatus = "UPDATING";

		boolean status = false;

		// transformando userListJSON em um array de DankiaFPTemplate
		Gson gson = new Gson();
		DankiaFPTemplate newUserList[] = gson.fromJson(userListJSON, DankiaFPTemplate[].class);

		// lista não vazia e não nula?
		if (newUserList.length > 0 && newUserList != null) {
			status = true;

			String strUserId = "";

			// percorrendo lista de usuarios
			for (DankiaFPTemplate fp : newUserList) {

				strUserId = String.valueOf(fp.getUserId());

				// insira a minucia no serviço da biometria
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
						errMsg = "AddMinutia: .Save() retornou false. Digital não foi adicionada.";
						insertLog(errMsg);
						return false;
					}
				} else {

					errMsg = "AddMinutia: Base64 de tamanho inválido:" + templateByteArray.length + ". UserId: "
							+ strUserId;
					insertLog(errMsg);
					return false;

				}

			} else {
				errMsg = "AddMinutia: Base64 não decodificado. Verifique o campo UserFingerPrintBase64 do UserId: "
						+ strUserId;
				insertLog(errMsg);
				return false;
			}

		} catch (Exception e) {
			e.printStackTrace();
			errMsg = "Exceção em addMinutia: " + org.apache.commons.lang.exception.ExceptionUtils.getStackTrace(e);
			generalStatus = "READY";
			insertLog(errMsg);
			return false;

		}

	}

	/**
	 * METODO PARA VERIFICAR SE A STRING RECEBIDA ESTÁ ENCRIPTADA EM BASE64
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
	 * @return ultima minucia cadastrada através da operação Enroll
	 */
	public String getEnrollMinutia() {
		return this.enrollMinutia;
	}

	/**
	 * OBTEM o último usuário identificado no Identify
	 * 
	 * @return Id do usuário identificado
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
