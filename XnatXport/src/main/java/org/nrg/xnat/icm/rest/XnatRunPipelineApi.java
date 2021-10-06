

package org.nrg.xnat.icm.rest;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import java.util.Arrays;
import org.slf4j.LoggerFactory;
import org.nrg.xnat.icm.plugin.XnatIntRunPipelinePlugin;
import java.util.zip.ZipEntry;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.Reader;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Collection;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RequestMethod;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import io.swagger.annotations.ApiOperation;
import java.util.Iterator;
import org.nrg.xft.security.UserI;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import org.nrg.xdat.om.XnatSubjectdata;
import java.util.ArrayList;
import java.io.OutputStream;
import java.util.zip.ZipOutputStream;
import org.nrg.xdat.om.base.BaseXnatProjectdata;
import java.io.FileInputStream;
import java.io.File;
import java.io.ByteArrayOutputStream;
import org.nrg.xnat.icm.utils.XnatToBidsUtils;
import org.nrg.xnat.icm.rest.*;
import java.util.Calendar;
import org.nrg.xft.XFTTable;
import org.nrg.xft.search.CriteriaCollection;
import org.nrg.xft.search.QueryOrganizer;
import java.util.LinkedList;
import org.nrg.xdat.XDAT;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PathVariable;
import javax.servlet.http.HttpServletResponse;
import java.util.List;
import org.slf4j.Logger;
import org.springframework.web.bind.annotation.RequestMapping;
import org.nrg.framework.annotations.XapiRestController;
import io.swagger.annotations.Api;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import java.util.*;
import java.io.*;
//import javax.ws.rs.WebApplicationException;
import java.lang.*;
import com.jcraft.jsch.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.io.PrintWriter;

@Api(description = "run pipeline on Cluster")
@XapiRestController
@RequestMapping({ "/int-run-pipline" })
public class XnatRunPipelineApi
{
    private static final Logger _logger;
    private static List<String> _xnatSessionTypes;
    private static String _dcm2niixExecutablePath;
    private String _tempBuildPath;
    private boolean _imageFormatAddDicoms;
    private boolean _imageFormatAddNiftis;
    private boolean _imageFormatAddBothImageFormats;



    public static String listImages[] = null;
	private static String allOrListSubject;
	



    // Paramètres création session cluster à distance : 

    private static int port = 22;
    private static ChannelExec channel = null;
    private static Session session = null;
    private static InputStream inputStream = null;
    private static OutputStream outputStream =null;
    private static ChannelShell channelShell = null;

    private static String fullPathScriptSlurm = "";
 	// Les paramètres à distance 
	
    public  static String user = "rahamani.a";
    private  static String passwordniolon = "";
    //private  static String host = "niolon.int.univ-amu.fr";
    private  static String host = "niolon02";

    private static String datTimeNow = "";
	private static String SCRIPT_SBATCH = "#!/bin/bash\n"
                                        + "#SBATCH -p batch\n";
    
    private static String NUMBER_OF_JOBS_PER_NODES = "#SBATCH --ntasks-per-node=1\n";
    private static String MAXIMAL_TIME_COMPUTATION = "#SBATCH -t 00:06:00\n";
    private static String NUMBER_OF_NODES = "#SBATCH -N 1\n";
    private static String STANDARD_OUTPUT_FILE = "#SBATCH -o ";   // à ajouter  le path vers l'output + un retour à la ligne ! !
    private static String STANDARD_ERROR_FILE = "#SBATCH -e ";    // à ajouter  le path vers l'error  + un retour à la ligne ! !
    private static String NAME_OF_SLURM_JOB = "#SBATCH -J";       // à ajouter l nom + un retour à la ligne
    private static String LOAD_MODULES = "\n\nmodule purge\n"
                                       + "module load all\n"
                                       + "\nmodule load anaconda/3\n";


    
    @ApiOperation(value = "Get list of piplines in cluster", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-pipelines/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void exportFiles(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("idCluster") final String idCluster, @RequestParam("password") final String password, @RequestParam("subject_ids") final String subject_ids)  throws IOException{
        final UserI xnatUser = XDAT.getUserDetails();
        ByteArrayOutputStream baos = null;
        final List<String> subjectsList = new LinkedList<String>();
       
        
        log("Runin pipeLine from [" + id_project + "]");
        

        log("id niolon = "+idCluster);

        user = idCluster;   

        //log("password = "+ password);

        passwordniolon = password;


        log("la liste séléctionné est   !! : "+subject_ids);

       

       /* Recupérer la list  séléctionné des sujet*/

        /*
       try {
            //System.out.println("Subjects = " + subject_ids);
            if ("all".equals(subject_ids)) {
                final QueryOrganizer qo = new QueryOrganizer("xnat:subjectData", xnatUser, "all");
                final CriteriaCollection cc = new CriteriaCollection("OR");
                cc.addClause("xnat:subjectData/project", (Object)id_project);
                cc.addClause("xnat:subjectData/sharing/share/project", (Object)id_project);
                qo.setWhere(cc);
                final String query = qo.buildQuery();
                final XFTTable table = XFTTable.Execute(query, xnatUser.getDBName(), xnatUser.getLogin());
                for (final Object[] array : table.rows()) {
                    final Object[] row = array;
                    for (final Object o : array) {
                        subjectsList.add(o.toString());
                    }
                }
            }
            else {
                final String[] split;
                final String[] tokens = split = subject_ids.split(",");
                for (final String token : split) {
                    subjectsList.add(token);
                }
            }


            final List<String> ssid = new ArrayList<String>();
                for (final String sid : subjectsList) {
                    log("subject   [" + sid + "]...");
                    final XnatSubjectdata xnatSubject = XnatSubjectdata.getXnatSubjectdatasById((Object)sid, xnatUser, false);
                    ssid.add(xnatSubject.getLabel());
                }
                final String listOfSubject = String.join(" ", ssid);
                log("list of subject " + listOfSubject);


        }catch ( Exception e) {
            e.printStackTrace();
        }*/

        
        
        String cle1 = "cle1";
        String valeur1 = "cow ";


        String cle2 = "cle2";
        String valeur2 = "fmriprep-20.0.4.sif";

        String cle3 = "cle3";
        String valeur3 = "fmriprep_20.2.3.simg";

        String cle4 = "cle4";
        String valeur4 = "macapype_v0.2.2.1";
        
        String cle5 = "cle5";
        String valeur5 = "macapype_v0.2.2.3";
        
        
        String cle6 = "cle6";
        String valeur6 = "mriqc_0.16.1.simg";
        
            

        //final SSHConnection sshConncetion = new SSHConnection();


        Map<String, String> _listPipelines  = new HashMap<>();

        
        
        try {
            
             doConnectionCluster(passwordniolon);

        }catch (InterruptedException e) { 
            e.printStackTrace();
                
         }catch (JSchException  jshe){


         }catch (IOException ioe){


         }
           
        
        
        
        for(int i= 0; i< listImages.length; i++){

        _listPipelines.put(listImages[i],listImages[i]);

        log("  "+listImages[i]+" :    "+listImages[i]);


        }

        /*

        _listPipelines.put(cle1,valeur1);
       
        _listPipelines.put(cle2,valeur2);

        _listPipelines.put(cle3,valeur3);
        
        _listPipelines.put(cle4,valeur4);
        
        _listPipelines.put(cle5,valeur5);
        
        _listPipelines.put(cle6,valeur6);
        
        */

        response.setContentType("application/json");
        
        response.setCharacterEncoding("UTF-8");
        
        JSONObject obj = new JSONObject();

        obj.putAll(_listPipelines);
        
        log(obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();
        

        log( "\n la date de maintenant est : "+getDateTimeNow() + "    \n\n");
 


        /*
        try {
            System.out.println("Subjects = " + subject_ids);
            if ("all".equals(subject_ids)) {
                final QueryOrganizer qo = new QueryOrganizer("xnat:subjectData", xnatUser, "all");
                final CriteriaCollection cc = new CriteriaCollection("OR");
                cc.addClause("xnat:subjectData/project", (Object)id_project);
                cc.addClause("xnat:subjectData/sharing/share/project", (Object)id_project);
                qo.setWhere(cc);
                final String query = qo.buildQuery();
                final XFTTable table = XFTTable.Execute(query, xnatUser.getDBName(), xnatUser.getLogin());
                for (final Object[] array : table.rows()) {
                    final Object[] row = array;
                    for (final Object o : array) {
                        subjectsList.add(o.toString());
                    }
                }
            }
            else {
                final String[] split;
                final String[] tokens = split = subject_ids.split("[,]");
                for (final String token : split) {
                    subjectsList.add(token);
                }
            }
            String fileName = "XNAT-" + id_project;
            if ("BIDS".equals(disk_tree_format_choice)) {
                fileName += "_BIDS";
            }
            
            else {
                if ("CENIR".equals(disk_tree_format_choice)) {
                    fileName += "_CENIR";
                }
                if (this._imageFormatAddDicoms || this._imageFormatAddBothImageFormats) {
                    fileName += "_DICOM";
                }
                if (this._imageFormatAddNiftis || this._imageFormatAddBothImageFormats) {
                    fileName += "_NIFTI";
                }
            }

            log("\n ");
            final Calendar now = Calendar.getInstance();
            String dateTime = "" + now.get(1);
            dateTime += "-";
            if (now.get(2) + 1 < 10) {
                dateTime += "0";
            }
            dateTime += now.get(2) + 1;
            dateTime += "-";
            if (now.get(5) < 10) {
                dateTime += "0";
            }
            dateTime += now.get(5);
            dateTime += "_";
            if (now.get(11) < 10) {
                dateTime += "0";
            }
            dateTime += now.get(11);
            dateTime += "h";
            if (now.get(12) < 10) {
                dateTime += "0";
            }
            dateTime = dateTime + now.get(12) + "";
            fileName = fileName + "-" + dateTime;
            this._tempBuildPath = XDAT.getSiteConfigPreferences().getBuildPath();
            this._tempBuildPath = this._tempBuildPath + "/" + id_project + "/xnat-icm-export-" + id_project;
            this._tempBuildPath = this._tempBuildPath + "-" + Calendar.getInstance().getTime().getTime();
            if ("BIDS".equals(disk_tree_format_choice)) {
                final XnatToBidsUtils xnatToBidsUtils = new XnatToBidsUtils(this._tempBuildPath);
                final String bidsZipFile = xnatToBidsUtils.convertToBidsFunction(id_project, subjectsList);
                baos = new ByteArrayOutputStream();
                final FileInputStream in = new FileInputStream(new File(bidsZipFile));
                final byte[] buffer = new byte[1024];
                int length;
                while ((length = in.read(buffer)) > 0) {
                    baos.write(buffer, 0, length);
                }
                in.close();
                baos.flush();
                baos.close();
            }
            else {
                this._imageFormatAddDicoms = "DICOM".equals(image_format_choice);
                this._imageFormatAddNiftis = "NIFTI".equals(image_format_choice);
                this._imageFormatAddBothImageFormats = "BOTH".equals(image_format_choice);
                if (this._imageFormatAddNiftis || this._imageFormatAddBothImageFormats) {
                    new File(this._tempBuildPath).mkdir();
                }
                final BaseXnatProjectdata projectData = (BaseXnatProjectdata)BaseXnatProjectdata.getProjectByIDorAlias(id_project, xnatUser, false);
                baos = new ByteArrayOutputStream();
                ZipOutputStream zos = new ZipOutputStream(baos);
                final List<String> ssid = new ArrayList<String>();
                for (final String sid : subjectsList) {
                    log("Exporting for subject [" + sid + "]...");
                    final XnatSubjectdata xnatSubject = XnatSubjectdata.getXnatSubjectdatasById((Object)sid, xnatUser, false);
                    ssid.add(xnatSubject.getLabel());
                }
                final String listOfSubject = String.join(" ", ssid);
                log("list of subject " + listOfSubject);
                zos = this.exportSubjectFiles(zos, id_project, fileName, subjectsList, ssid);
                zos.finish();
                zos.close();
            }
            final File directoryToZip = new File("/data/xnat/tmp/" + fileName);
            final List<File> fileList = new ArrayList<File>();
            log("---Getting references to all files in: " + directoryToZip.getCanonicalPath());
            log("---Creating zip file");
            this.zip2(fileName);
            log("---Done");
            response.setContentType("application/zip");
            response.setHeader("Content-Disposition", "attachment; filename=" + fileName + ".tar.gz");
            final File file = new File("/data/xnat/tmp/" + fileName + ".tar.gz");
            final FileInputStream fileIn = new FileInputStream(file);
            System.out.println("Writing to output stream..");
            final byte[] outputByte = new byte[4096];
            while (fileIn.read(outputByte, 0, 4096) != -1) {
                response.getOutputStream().write(outputByte, 0, 4096);
            }
            fileIn.close();
            clean(fileName);
            response.flushBuffer();
        }
        catch (Exception e) {
            e.printStackTrace();
            if (baos != null) {
                try {
                    baos.close();
                }
                catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
            if (!"".equals(this._tempBuildPath)) {
                try {
                    FileUtils.deleteDirectory(new File(this._tempBuildPath));
                    log("Temporary folder [" + this._tempBuildPath + "] deleted");
                }
                catch (IOException ioe) {
                    ioe.printStackTrace();
                }
            }
        }
        finally {
            if (baos != null) {
                try {
                    baos.close();
                }
                catch (Exception e2) {
                    e2.printStackTrace();
                }
            }
            if (!"".equals(this._tempBuildPath)) {
                try {
                    FileUtils.deleteDirectory(new File(this._tempBuildPath));
                    log("Temporary folder [" + this._tempBuildPath + "] deleted");
                }
                catch (IOException ioe2) {
                    ioe2.printStackTrace();
                }
            }
        }
        log("Exporting done for project [" + id_project + "].");
        log("**************************** ");
        log("**************************** ");
        
        log("**************************** ");
        log("**************************** ");

        */

    }


    /* Cette route permet de lancer le pipeline voulu dans le cluster de calcul */

    @ApiOperation(value = "Start piplines in cluster", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/start-pipeline/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody
    public void startPipelineInCluster(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectPipeline") final String selectPipeline,  @RequestParam("idCluster") final String idCluster, @RequestParam("subject_ids") final String subject_ids, @RequestParam("nameExportDir") final String nameExportDir)  throws IOException{

        final UserI xnatUser = XDAT.getUserDetails();
        final List<String> subjectsList = new LinkedList<String>();
        String listOfSubjectWithSpaceSeparated = null;
        String listOfSubjectWithCamasSeparated = "";
        String SCRIPT_SBATCH_GLOBAL = "";
        allOrListSubject = subject_ids ;


        log("** Start  pipeline **\n");
        
        log("Runin pipeLine from [" + id_project + "]");

        log(" vous avez selectionné le pojet ou sujets :  : [" + subject_ids + "]");

        log("le export path param   est : " +nameExportDir );



        try {
            //System.out.println("Subjects = " + subject_ids);
            if ("all".equals(subject_ids)) {
                final QueryOrganizer qo = new QueryOrganizer("xnat:subjectData", xnatUser, "all");
                final CriteriaCollection cc = new CriteriaCollection("OR");
                cc.addClause("xnat:subjectData/project", (Object)id_project);
                cc.addClause("xnat:subjectData/sharing/share/project", (Object)id_project);
                qo.setWhere(cc);
                final String query = qo.buildQuery();
                final XFTTable table = XFTTable.Execute(query, xnatUser.getDBName(), xnatUser.getLogin());
                for (final Object[] array : table.rows()) {
                    final Object[] row = array;
                    for (final Object o : array) {
                        subjectsList.add(o.toString());
                    }
                }
            }
            else {
                final String[] split;
                final String[] tokens = split = subject_ids.split(",");
                for (final String token : split) {
                    subjectsList.add(token);
                }
            }


            final List<String> ssid = new ArrayList<String>();
                for (final String sid : subjectsList) {
                    log("subject   [" + sid + "]...");
                    final XnatSubjectdata xnatSubject = XnatSubjectdata.getXnatSubjectdatasById((Object)sid, xnatUser, false);
                    ssid.add(xnatSubject.getLabel());
                }
                final String listOfSubject = String.join(" ", ssid);
                listOfSubjectWithSpaceSeparated = listOfSubject;
                listOfSubjectWithCamasSeparated = String.join(",",ssid);
                log("list of subject " + listOfSubject);


        }catch ( Exception e) {
            e.printStackTrace();
        }


        log("Le pipeline Choisi est : " + selectPipeline + "");



        log("Le script généré est  : \n ");

        datTimeNow = getDateTimeNow();

        SCRIPT_SBATCH_GLOBAL = SCRIPT_SBATCH_GLOBAL 
            + SCRIPT_SBATCH  
            + NUMBER_OF_JOBS_PER_NODES
            + NUMBER_OF_NODES
            + STANDARD_OUTPUT_FILE + "./" + selectPipeline + "_" + id_project + "_" + datTimeNow + ".out"
            + "\n" + STANDARD_ERROR_FILE + "./" + selectPipeline + "_" + id_project + "_" + datTimeNow + ".err"
            + "\n" + NAME_OF_SLURM_JOB + " " + idCluster + "_" + selectPipeline
            + "\n" + getOtherParamatersSbatch(selectPipeline)
            + "\n" + LOAD_MODULES 
            + "\n" + "" + commandeDownloadData(listOfSubjectWithCamasSeparated, id_project, nameExportDir, listOfSubjectWithSpaceSeparated)
            + "\n" + whichCommandSingularity(selectPipeline, nameExportDir + "/" + id_project + "BIDS");
            
            log(SCRIPT_SBATCH_GLOBAL);

        

        final String namFileGenerated = generateFIleScripte(SCRIPT_SBATCH_GLOBAL, selectPipeline, idCluster);

        log( "le fichier a envoyer est  " + namFileGenerated );
        
       
        
        try{

           sendFileToCluster(passwordniolon,namFileGenerated);

        }catch (InterruptedException e) { 
            
            e.printStackTrace();
                
        }catch (JSchException  jshe){


        }catch (IOException ioe){


        }catch (SftpException sftpe){
            
        }  
        
        log("done !");
        


    }


    



    /* la méthode qui permet de  recupere le contenu la liste des images singularity sur niolon */
    public 	static  String[] recupererListImgCluster(String resultConsole){
    	
    	listImages = resultConsole.split("\\r?\\n");
        System.out.println("\n la taille de tableau lines est  : "+listImages.length +"\n");
         
    
    	return listImages;
    }
    


    /* Cette a pour fonction d'établir une connection avec cluster de calcul */
    public void doConnectionCluster(String password) throws   InterruptedException, JSchException, IOException  {
		
        String remoteFile = "ls";
        
        //String  commande= "niolon_interactive; cd /hpc/crise/rahmani.a/script; ls";
        
        try {

            JSch.setLogger(new MyLogger());
            
            JSch jsch = new JSch();
           
            
        /*  
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");

            config.put("StrictHostKeyChecking", "no");
            config.put("PreferredAuthentications", "publickey,keyboard-interactive,password");
        */

            
            Properties config = new Properties();
            
            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");

            //session.setConfig("cipher.s2c", "aes128-ctr");
            //session.setConfig("cipher.c2s", "aes128-ctr");
            
            config.put("StrictHostKeyChecking", "no");
            
            session = jsch.getSession(user, host, port);
            
            //jsch.addIdentity("/var/lib/tomcat08/.ssh");
            
           
        
            session.setPassword(password);
        
            
            
            session.setConfig(config);
              
            
            System.out.println("Establishing Connection...");
        
            session.connect();
        
            System.out.println("Connection established.");
            
            log("********************** \n ");

            log("Connection established");
            
            // Execution de la commande
            
            channel = (ChannelExec) session.openChannel("exec");
            
            channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_images");
            //channel.setCommand("ls");
            
            while (channel.isConnected()) {
                Thread.sleep(1000);
            }
            
            //channel.setCommand("cd /hpc/crise/rahmani.a/script ");
           // channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_images");
            
            ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
            channel.setOutputStream(responseStream);
            channel.connect();
            
            System.out.println(" ************************************************ ");

            log("*************************");
            
            while (channel.isConnected()) {
                Thread.sleep(100);
            }
            
            String responseString = new String(responseStream.toByteArray());
            System.out.println(responseString);
            
            /* On appel la fonction de recupération de l'enssemble des images*/
            log(recupererListImgCluster(responseString).toString());
            
			
			
			/*
            
            System.out.println("Crating SFTP Channel.");
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            sftpChannel.connect();
            System.out.println("SFTP Channel created.");

            InputStream inputStream = sftpChannel.get(remoteFile);
            
            try (Scanner scanner = new Scanner(new InputStreamReader(inputStream))) {
                while (scanner.hasNextLine()) {
                    String line = scanner.nextLine();
                    System.out.println(line);
                }
            }*/
            
        } catch (JSchException e) {
            e.printStackTrace();

        }finally {
            if (session != null) {
                session.disconnect();
                //outputStream.close();
				//inputStream.close();
				
            }
            if (channel != null) {
                channel.disconnect();
                //channelShell.disconnect();
            }
        }
	

	
    }

    /* Cette fonction à pour but d'envoyer le fichier généré au cluster de calcul */
    public  void sendFileToCluster(String password, String nameFileGenerated) throws InterruptedException, JSchException, IOException, SftpException {

        /* Chemin vers le local file */
        String localFile = fullPathScriptSlurm;
        
        log("Le fichier à envoyer est : " + fullPathScriptSlurm );
        /* Chemin où sera  transmit le fichier dans le cluster */
        String remoteDir = "/home/rahamani.a"; 
        
        try {
            
            JSch.setLogger(new MyLogger());
        
            JSch jsch = new JSch();



            Properties config = new Properties();
            
            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");

            //session.setConfig("cipher.s2c", "aes128-ctr");
            //session.setConfig("cipher.c2s", "aes128-ctr");
            
            config.put("StrictHostKeyChecking", "no");

            
            
            session = jsch.getSession(user, "niolon.int.univ-amu.fr", port);
        
            session.setPassword(password);
            

            session.setConfig(config);

            log("Establishing Connection...");
        
            session.connect();
        
            log("Connection established.");

            System.out.println("Crating SFTP Channel.");
            
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            
            sftpChannel.connect();
            
            log("SFTP Channel created.");
            
            /* la méthod  put pour envoyer le fichier */
            sftpChannel.put(localFile, remoteDir);

            sftpChannel.put("/var/lib/tomcat8/Bureau/xnat2bids_reconstruct.py", remoteDir);


            
            log(" submit  file with sucess ");
            
            sftpChannel.exit();

           
            channelShell = (ChannelShell) session.openChannel("shell");
            
            inputStream = channelShell.getInputStream();//The data arriving from the far end can be read from this stream.
            
            channelShell.setPty(true);
            
            channelShell.connect();
            
            log ("sell  channel is opned ! ");

            outputStream = channelShell.getOutputStream();
            
            PrintWriter printWriter = new PrintWriter(outputStream);
            
            printWriter.println("chmod +x " + remoteDir + "/" + nameFileGenerated);

            log("le scripte slum reçu est ====> "+remoteDir + "/" + nameFileGenerated);
            
            printWriter.println(" sbatch ./"+nameFileGenerated);
             

            printWriter.flush () ; // force the buffer data output
        
            
            byte[] tmp = new byte[1024];
            while(true){
                
                while(inputStream.available() > 0){
                    int i = inputStream.read(tmp, 0, 1024);
                    if(i < 0) break;
                    String s = new String(tmp, 0, i);
                    if(s.indexOf("--More--") >= 0){
                        outputStream.write((" ").getBytes());
                        outputStream.flush();
                    }
                    //System.out.println(s);
                }
                if(channelShell.isClosed()){
                    System.out.println("exit-status:"+channelShell.getExitStatus());
                    break;
                }
                try{
                    Thread.sleep(1000);
                }catch(Exception e){
                    
                }
                
            }

        }catch (JSchException e) {

            e.printStackTrace();

        }finally {
            
            if (session != null) {
                session.disconnect();
                outputStream.close();
                inputStream.close();
                    
                }
            if (channel != null) {
                channel.disconnect();
                channelShell.disconnect();
            }
           
        }



    }



    public static String getDateTimeNow(){

        final Calendar now = Calendar.getInstance();
        String dateTime = "" + now.get(1);
        dateTime += "-";
        if (now.get(2) + 1 < 10) {
            dateTime += "0";
        }
        dateTime += now.get(2) + 1;
        dateTime += "-";
        if (now.get(5) < 10) {
            dateTime += "0";
        }
        dateTime += now.get(5);
        dateTime += "_";
        if (now.get(11) < 10) {
            dateTime += "0";
        }
        dateTime += now.get(11);
        dateTime += "h";
        if (now.get(12) < 10) {
            dateTime += "0";
        }
        dateTime = dateTime + now.get(12) + "";

        return  dateTime; 

    }



    // A partir de cette méthode on génère le fichier final à soumetre au cluster
    // Penser à passer le nom de projet en paramétre
    public static String generateFIleScripte(String codeSbatch, String pipeLineSelected, String userIdCluster) {

      String    dirOutputpath = "/data/xnat/tmp/";
      String suiteName = userIdCluster + "_" + pipeLineSelected + "_" + datTimeNow + ".sh";
      
      /* On concerver le nom  pour récupérer le bon fichier */
      fullPathScriptSlurm = dirOutputpath+suiteName;


      File f = new File(dirOutputpath+suiteName);
      
      //f.setExecutable(true);
      
      System.out.println(f.exists());
      log("le fichier à été créer ! ");

      if(!f.getParentFile().exists()) {
          f.getParentFile().mkdirs();
          
      }        

      if(!f.exists()) {
         try {
            f.createNewFile();
         } catch (Exception e) {
            e.printStackTrace();
         }        
      }

      try {
         File dir = new File(f.getParentFile(), f.getName());
         PrintWriter pWriter = new PrintWriter(dir);
         // Ajouter le contenu du script au fichier slurm 
         pWriter.print(codeSbatch);
         //pWriter.print("writing anything..    . \n");
         //pWriter.print("writing anything..    .");
         
         pWriter.close();
      } catch (FileNotFoundException e) {
         e.printStackTrace();
      }
        try{

            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec("chmod +777 " + fullPathScriptSlurm );

        }catch (Exception e) {
                log(e.toString()); 
        }      

        return suiteName;
    }


    public static String  commandeDownloadData(String subjectSelected, String projectName, String dirIputdata, String lisSubjectWithSpaceSeparated){
        
        String filenameTxt = "\"" + dirIputdata + "/download_commandLine.txt" +"\"";
        
        String filenameCsv = "\"" + dirIputdata + "/download_report.csv" +"\"";

        String dirDataInBIDS =  dirIputdata + "/" + projectName + "BIDS";
        
        String commande = "source activate dax_env\n"
                + "\n" + "filenameTxt=" + filenameTxt
                + "\n" + "filenameCsv=" + filenameCsv
                + "\n \n" + "mkdir -p " + dirIputdata
                + "\n" + "Xnatdownload -p " + projectName + " -d " + dirIputdata + " --subj ";

          if(!allOrListSubject.equals("all")){

            commande += subjectSelected + "  -s all --rs all \n";

          }else {
              
            commande += " all -s all --rs all \n";
              
          } 

          commande += "\n"
                    + "\n" +  "if [ -f $filenameTxt  ]; then"
                    + "\n" +  "   rm $filenameTxt"  
                    + "\n" +  "   echo \"download_txt removed\""
                    + "\n" +  "fi"
                    + "\n" +  "if [ -f $filenameCsv  ]; then"
                    + "\n" +  "   rm $filenameCsv"  
                    + "\n" +  "   echo \"download_csv removed\""
                    + "\n" +  "fi"
                    + "\n" +  "\n" + "\n" + "\n"
                    + "\n" +  "mkdir -p " + dirDataInBIDS + "\n"
                    + "\n" +  "python xnat2bids_reconstruct.py " + dirIputdata + "/" + projectName + " " + dirDataInBIDS +  " " + lisSubjectWithSpaceSeparated +"\n";
                    
            
          commande += "\nsource deactivate \n" + "\nmodule load singularity_images" + "\n"; 

        // faut supprimer les fichier générer par Xnatdownload
        return commande;

    }

    /* Cette methode renvoi d'autre paramétre sbatch a rajouter au script en slurm */
    public String getOtherParamatersSbatch(String selectPipeline){
        
        String commande = ""; 

        switch(selectPipeline){

            case "fmriprep_20.2.3.simg" :
               commande += "\n" + "#SBATCH --mem=48gb"
                         + "\n" + "#SBATCH --cpus-per-task=64"
                         + "\n" + "#SBATCH --time=20:00:00"
                         + "\n";
                break;

            case "   " : 

                break;

            default:
                commande = "";
        }

        return commande;
    }



    public String whichCommandSingularity(String whichPipeline, String inputDirBIDS){

        if (whichPipeline.equals("fmriprep_20.2.3.simg")) {

            return getCommandeFmriPrepSimg(whichPipeline, inputDirBIDS);

        }

        if (whichPipeline.equals("macapype_v0.2.2.3")) {

            return getCommandeMacapype(whichPipeline, inputDirBIDS);
        }

        else return "";

    }


    public String  getCommandeMacapype(String version, String inputDirBIDS){

        String commande = "\n" + "singularity run -B " + inputDirBIDS + ":/data/macapype ";
        commande += "/hpc/shared/apps/x86_64/softs/singularity_images/" + version + " ";

        commande += "python /opt/packages/macapype/workflows/segment_pnh.py -data /data/macapype/ -out /data/macapype/outputSingularity -soft ANTS -params /opt/packages/macapype/workflows/params_segment_macaque_ants_based.json\n";
    
    return commande;
    }
    

    public String getCommandeFmriPrepSimg(String version , String inputDirBIDS){

        String commande = "\n" + "singularity run --cleanenv -B " + inputDirBIDS + ":/work_dir "
                        + "-B /hpc/shared/apps/x86_64/softs/freesurfer/7.1.1:/license_path "
                        + "/hpc/shared/apps/x86_64/softs/singularity_images/" + version + " "
                        + "--fs-license-file /license_path/.license "
                        + "/workdir /workdir/derivatives/fmriprep "
                        + "participant --participant-label 01 -w /work_dir/temp_data_test/ "
                        + "--cifti-output --low-mem --mem-mb 32000 --nthreads 64";

        return commande; 
    }


    public ZipOutputStream exportSubjectFiles(final ZipOutputStream zos, final String id_project, final String fileName, final List<String> subjectsList, final List<String> listOfSubject) {
        final UserI xnatUser = XDAT.getUserDetails();
        try {
            final File FolderFile = new File("/data/xnat/tmp/" + fileName);
            FolderFile.mkdirs();
            try {
                final String program = "python";
                final String programName = "xnat2bids_reconstruct.py";
                final String inputDir = XDAT.getSiteConfigPreferences().getArchivePath() + "/" + id_project;
                final String outputDir = FolderFile.toString();
                final String _tempProjectPath = XDAT.getSiteConfigPreferences().getArchivePath();
                final List<String> cmd = new ArrayList<String>();
                cmd.add(program);
                cmd.add(programName);
                cmd.add(inputDir);
                cmd.add(outputDir);
                cmd.addAll(listOfSubject);
                final ProcessBuilder pb = new ProcessBuilder(cmd);
                final Process p = pb.start();
                final InputStreamReader isr = new InputStreamReader(p.getInputStream());
                final BufferedReader br = new BufferedReader(isr);
                String ligne = "";
                while ((ligne = br.readLine()) != null) {
                    log(ligne);
                }
            }
            catch (Exception e) {
                log(e.toString());
            }
            return zos;
        }
        catch (Exception e2) {
            e2.printStackTrace();
            return null;
        }
    }
    

    public static void getAllFiles(final File dir, final List<File> fileList) {
        try {
            final File[] listFiles;
            final File[] files = listFiles = dir.listFiles();
            for (final File file : listFiles) {
                fileList.add(file);
                if (file.isDirectory()) {
                    System.out.println("directory:" + file.getCanonicalPath().toString());
                    getAllFiles(file, fileList);
                }
                else {
                    System.out.println("     file:" + file.getCanonicalPath().toString());
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public ZipOutputStream writeZipFile(final File directoryToZip, final List<File> fileList) {
        try {
            final FileOutputStream fos = new FileOutputStream("/data/xnat/tmp/" + directoryToZip.getName() + ".tar.gz");
            log("Path file export : "+directoryToZip.getName());
            final ZipOutputStream zos = new ZipOutputStream(fos);
            for (final File file : fileList) {
                if (!file.isDirectory()) {
                    addToZip(directoryToZip, file, zos);
                }
            }
            zos.close();
            fos.close();
            return zos;
        }
        catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        }
        catch (IOException e2) {
            e2.printStackTrace();
            return null;
        }
    }
    
    public static void addToZip(final File directoryToZip, final File file, final ZipOutputStream zos) throws FileNotFoundException, IOException {
        final FileInputStream fis = new FileInputStream(file);
        final String zipFilePath = file.getCanonicalPath().substring(directoryToZip.getCanonicalPath().length() + 1, file.getCanonicalPath().length());
        System.out.println("Writing " + zipFilePath.toString() + " to zip file");
        final ZipEntry zipEntry = new ZipEntry(zipFilePath);
        zos.putNextEntry(zipEntry);
        final byte[] bytes = new byte[4096];
        int length;
        while ((length = fis.read(bytes)) >= 0) {
            zos.write(bytes, 0, length);
        }
        zos.closeEntry();
        fis.close();
    }
    
    public static void clean(final String fileName) throws FileNotFoundException, IOException {
        FileUtils.deleteDirectory(new File("/data/xnat/tmp/" + fileName));
        FileUtils.forceDelete(new File("/data/xnat/tmp/" + fileName + ".tar.gz"));
    }
    
    public void zip2(final String fileName) {
        try {
            final ProcessBuilder pb = new ProcessBuilder(new String[] { "python", "zip.py", "" + fileName });
            final Process p = pb.start();
            final InputStreamReader isr = new InputStreamReader(p.getInputStream());
            final BufferedReader br = new BufferedReader(isr);
            String ligne = "";
            while ((ligne = br.readLine()) != null) {
                log(ligne);
            }
        }
        catch (Exception e) {
            log(e.toString());
        }
    }
    
    private static void log(final String logString) {
        System.out.println(logString);
        XnatRunPipelineApi._logger.info(logString);
    }
    
    static {
        _logger = LoggerFactory.getLogger((Class)XnatIntRunPipelinePlugin.class);
        XnatRunPipelineApi._xnatSessionTypes = Arrays.asList("xnat:mrSessionData", "xnat:crSessionData", "xnat:ctSessionData", "xnat:petSessionData", "xnat:petmrSessionData");
        XnatRunPipelineApi._dcm2niixExecutablePath = "dcm2niix";
    }


    // to print  logger in file.log
    public static class MyLogger implements com.jcraft.jsch.Logger {
    
    static java.util.Hashtable name=new java.util.Hashtable();
    static{
      name.put(new Integer(DEBUG), "DEBUG: ");
      name.put(new Integer(INFO), "INFO: ");
      name.put(new Integer(WARN), "WARN: ");
      name.put(new Integer(ERROR), "ERROR: ");
      name.put(new Integer(FATAL), "FATAL: ");
    }
    public boolean isEnabled(int level){
      return true;
    }
    public void log(int level, String message){
      System.err.print(name.get(new Integer(level)));
      System.err.println(message);
    }
  }

}






