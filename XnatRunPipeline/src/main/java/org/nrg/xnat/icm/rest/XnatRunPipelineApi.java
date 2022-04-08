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
import java.io.InputStream;
import java.io.ObjectInputStream.GetField;
import java.io.PrintWriter;
import java.io.FileReader;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.json.simple.*;

import org.json.simple.parser.JSONParser;
/* Les importations de SSHJ 

import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.xfer.FileSystemFile;

*/


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
 	// Les paramètres du cluster à distance 
    public  static String user = "";
    private  static String passwordniolon = "";
    //private  static String host = "niolon.int.univ-amu.fr";
    private  static String host = "niolon02";

    private static String ID_PROJECT = "";
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

    private static String GET_USER_ENV = "\n#SBATCH --get-user-env=L\n";
    private static String LOAD_IMG_SINGULARITY = "";
    private static String ADDITIONAL_PARAMS = "\n";

    public static String ID_CLUSTER = "";

    public static String URI_HOST_XNAT = "--host http://10.164.0.82:8080 ";

    public static String CHANGE_WORKING_DIRECTORY = "#SBATCH --chdir=/tmp\n";

    // Path to config file xnat
		
	private static String config_file_xnat = "xnat_config_file_V1.json"; 
	


    public static JSONObject jsonObject;
    public static ArrayList<String> listKeysJsonFile = null;

    
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

        /* Read config file and initialize  params */ 
       /*
       try {

            readConfigFileJson();
       
       } catch (IOException ioe){

       } catch (Exception e){


       }*/

       

       /* Recupérer la list  séléctionné des sujet */

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

        
        
        /*try {
            /*
            doConnectionCluster(passwordniolon);
            */

            String linkImgeSingularity = (String) jsonObject.get("linkAllImgSingularity");
            String dirName = "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps";
            
            File fileName = new File(linkImgeSingularity);
            
            File[] fileList = fileName.listFiles();
            
            listImages = new String[fileList.length];


            
            for(int i= 0; i< listImages.length; i++){

                listImages[i] = fileList[i].getName();

                _listPipelines.put(listImages[i],listImages[i]);

                log("  "+listImages[i]+" :    "+listImages[i]);

            }


                /*         }catch (InterruptedException e) { 
                e.printStackTrace();
                    
            }catch (JSchException  jshe){


            }catch (IOException ioe){


            } */

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
        
        

        log( "\n La date de maintenant est : "+getDateTimeNow() + "    \n\n");
 


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
    public void startPipelineInCluster(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectPipeline") final String selectPipeline,  @RequestParam("idCluster") final String idCluster, @RequestParam("subject_ids") final String subject_ids, @RequestParam("nameExportDir") final String nameExportDir, 
                 @RequestParam("additionalParams") final String additionalParams)  throws IOException{

        final UserI xnatUser = XDAT.getUserDetails();
        final List<String> subjectsList = new LinkedList<String>();
        String listOfSubjectWithSpaceSeparated = null;
        String listOfSubjectWithCamasSeparated = "";
        String SCRIPT_SBATCH_GLOBAL = "";
        String pipeLineSelected = "";
        allOrListSubject = subject_ids ;
        ID_PROJECT =  id_project;

        ID_CLUSTER = idCluster ;



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

        pipeLineSelected = (String) ((JSONObject) getJsonObjectByKey(jsonObject,selectPipeline)).get("name");

        log("Vous avez choisi le pipeline : " +pipeLineSelected + "\n");
        log("Le script généré est  : \n ");

        datTimeNow = getDateTimeNow();

        sifOrSimg(selectPipeline);
        
        if (!additionalParams.equals(null)){
            ADDITIONAL_PARAMS = additionalParams;
        } else {
            ADDITIONAL_PARAMS = "\n";
        }
        
        SCRIPT_SBATCH_GLOBAL = SCRIPT_SBATCH_GLOBAL 
            + SCRIPT_SBATCH  
            + NUMBER_OF_JOBS_PER_NODES
            + NUMBER_OF_NODES
            + CHANGE_WORKING_DIRECTORY
            + GET_USER_ENV
            + STANDARD_OUTPUT_FILE + "/home/"+ idCluster  + "/xnat-batch-scripts/"+ selectPipeline + "_" + id_project + "_" + datTimeNow + ".out"
            + "\n" + STANDARD_ERROR_FILE + "/home/"+ idCluster  + "/xnat-batch-scripts/"+  selectPipeline + "_" + id_project + "_" + datTimeNow + ".err"
            + "\n" + NAME_OF_SLURM_JOB + " " + idCluster + "_" + selectPipeline
            + "\n" + getOtherParamatersSbatch(selectPipeline)
            + "\n" + LOAD_MODULES 
            + "\n" + "" + commandeDownloadData(listOfSubjectWithCamasSeparated, id_project, nameExportDir, listOfSubjectWithSpaceSeparated)
            + "\n" + LOAD_IMG_SINGULARITY
            + "\n" + whichCommandSingularity(selectPipeline, nameExportDir)
            + "  "  + additionalParams + "\n";
            
            log(SCRIPT_SBATCH_GLOBAL);

        

        final String namFileGenerated = generateFIleScripte(SCRIPT_SBATCH_GLOBAL, selectPipeline, idCluster);

        log( "Le fichier a envoyer est  " + namFileGenerated );
        

        // To send file to the cluster

        /*
        try{

           sendFileToCluster(passwordniolon,namFileGenerated);
           log("\nLe fichier a été envoyer avec succé");

        }catch (InterruptedException e) { 
            
            e.printStackTrace();
                
        }catch (JSchException  jshe){


        }catch (IOException ioe){


        }catch (SftpException sftpe){
            
        } 
        */
        
        log("done !");
        
        

    }




    /* Cette fonction permet de récuperer les la liste des sessions corréspondant à un sujet */
    @ApiOperation(value = "Get list of session in subject", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-sessions/{id_project}" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody  
    public void exportFiles(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("selectedSubject") final String selectedSubject)  throws IOException{
        final UserI xnatUser = XDAT.getUserDetails();
        ByteArrayOutputStream baos = null;
        final List<String> subjectsList = new LinkedList<String>();
        Map<String, String> listSessionOfSubject  = new HashMap<>();
        String [] listAllsession = null; // mettre toute les sessions d'un projet 
        
        String dirName = "/data/xnat/archive/" + id_project + "/arc001";

        log("Vous avez choisi  [ " + id_project + "] et sujet : [ " + selectedSubject + "]");
        
        File fileName = new File(dirName);
        
        File[] fileList = fileName.listFiles();
        
        listAllsession = new String[fileList.length];


                    
            for(int i= 0; i< listAllsession.length; i++){

                listAllsession[i] = fileList[i].getName();
                // tester si le nom commence par le préfix du sujet
                if (listAllsession[i].startsWith(selectedSubject + "_"))
                {
                    listSessionOfSubject.put(listAllsession[i],listAllsession[i]);

                    log("  "+listAllsession[i]+" :    "+listAllsession[i]);
                }

            }

        // Préparer le listSessionOfSubject à envoyer en json

        response.setContentType("application/json");
        
        response.setCharacterEncoding("UTF-8");
        
        JSONObject obj = new JSONObject();

        obj.putAll(listSessionOfSubject);
        
        log("les sessions à envoyer sont : " +obj.toString());

        PrintWriter out = response.getWriter();

        out.print(obj);

        out.flush();
    
    }


    /* Cette fonction permet de récuperer les la liste des sessions corréspondant à un sujet */
    @ApiOperation(value = "Get list of team names", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Connection success "), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/get-team-names" }, produces = { "application/json" }, method = { RequestMethod.POST })
    @ResponseBody  
    public void getTeamNames(final HttpServletResponse response)  throws IOException{
    
       /* Read config file and initialize  params */ 
        try {

            readConfigFileJson();
       
        } catch (IOException ioe){

        } catch (Exception e){


       }
       
       
       
       
        JSONArray jsonArray = (JSONArray) jsonObject.get("teamNames");


        response.setContentType("application/json");
        
        response.setCharacterEncoding("UTF-8");
        

        log("Les noms des équipes à envoyer sont : " +jsonArray.toString());

        PrintWriter out = response.getWriter();

        out.print(jsonArray);

        out.flush();
    


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
            
            channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_BIDSApps");
            //channel.setCommand("ls");
            
            while (channel.isConnected()) {
                Thread.sleep(1000);
            }
            
            //channel.setCommand("cd /hpc/crise/rahmani.a/script ");
           // channel.setCommand("ls /hpc/shared/apps/x86_64/softs/singularity_BIDSApps");
            
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
        String remoteDir = "/home/"+ID_CLUSTER; 
        
        try {
                final String program = "sudo";
                final String programName = "/opt/bin/xnat-rpd.sh" ;
                final String inputDir = "-u";
                final String option1 = ID_CLUSTER;
                final String option2 = "-f";
                final String option3 = localFile;
                final List<String> cmd = new ArrayList<String>();
                cmd.add(program);
                cmd.add(programName);
                cmd.add(inputDir);
                cmd.add(option1);
                cmd.add(option2);
                cmd.add(option3);    
                log(cmd.toString());

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







        
                
/*         try {
            
            JSch.setLogger(new MyLogger());
        
            JSch jsch = new JSch();

            //jsch.setKnownHosts("~/.ssh/known_hosts");
            Properties config = new Properties();
            
            //config.put("kex","curve25519-sha256, curve25519-sha256@libssh.org, diffie-hellman-group16-sha512, diffie-hellman-group18-sha512, diffie-hellman-group-exchange-sha256");

            config.put("kex", "diffie-hellman-group1-sha1,diffie-hellman-group14-sha1,diffie-hellman-group-exchange-sha1,diffie-hellman-group-exchange-sha256");

            //session.setConfig("cipher.s2c", "aes128-ctr");
            //session.setConfig("cipher.c2s", "aes128-ctr");
            
            config.put("StrictHostKeyChecking", "no");

            
            
            session = jsch.getSession(user, "niolon.int.univ-amu.fr", port);
        
            session.setPassword(password);
            

            session.setConfig(config);

            log("Establishing Connection...");

            session.connect();
            
        
            log("Connection established...");

            System.out.println("Crating SFTP Channel...");
            
            ChannelSftp sftpChannel = (ChannelSftp) session.openChannel("sftp");
            
            
            try {

                sftpChannel.connect();

            } catch (Exception e) {
                throw new JSchException("Unable to connect to SFTP server. " + e.toString());
            }
            
            
           // sftpChannel.connect();
            
            log("SFTP Channel created.");
            
            // la méthod  put pour envoyer le fichier 
            sftpChannel.put(localFile, remoteDir);

            //sftpChannel.put("/var/lib/tomcat8/Bureau/xnat2bids_reconstruct.py", remoteDir);


            
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
            } */



           
        }
         


/*         final SSHClient sshClient = new SSHClient();
        sshClient.loadKnownHosts();
        sshClient.connect("pharo.int.univ-amu.fr");






        try {
            sshClient.authPassword(user, password);
            //final Session session1 = sshClient.startSession();
            try {

                sshClient.authPublickey(System.getProperty(user));
                //final String src = System.getProperty("user.home") + File.separator + "test_file";
                sshClient.newSCPFileTransfer().upload(new FileSystemFile(localFile),remoteDir); 


            } finally {
                //session1.close();
            }
        } finally {
            sshClient.disconnect();

                
    } */

    

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

    /* pour détérminer si l'img termine par .sif ou  */
    public void sifOrSimg(String imageSingularity){

        if (imageSingularity.contains(".sif")){

        LOAD_IMG_SINGULARITY = "\nmodule load singularity\n";

        }
        else {

            LOAD_IMG_SINGULARITY = "\n\n";

        }


    }



    // A partir de cette méthode on génère le fichier final à soumetre au cluster
    // Penser à passer le nom de projet en paramétre
    public static String generateFIleScripte(String codeSbatch, String pipeLineSelected, String userIdCluster) {

      String    dirOutputpath = "/tmp/";
      String suiteName = userIdCluster + "_" + pipeLineSelected + "_" + datTimeNow + ".sh";
      
      /* On concerver le nom  pour récupérer le bon fichier */
      fullPathScriptSlurm = dirOutputpath+suiteName;


      File f = new File(dirOutputpath+suiteName);
      
      //f.setExecutable(true);
      
      System.out.println(f.exists());
      log("le fichier a été créer ! ");

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


    public static void readConfigFileJson() throws Exception, IOException {

        listKeysJsonFile = new ArrayList<String>();
		// Parse json file
		JSONParser parser = new JSONParser();
		
		try {
			
	
		 jsonObject =  (JSONObject) parser.parse(new FileReader(config_file_xnat));
		 
		 JSONObject aaa =  (JSONObject) getJsonObjectByKey(jsonObject, "qsiprep_0.14.3.sif");
		 
		 System.out.println("la taille de l'objet qsiprep_0.14.3.sif est :" + aaa.size());
		 
		 String key_outputDirectory = "outputDirectory";
		
		 System.out.println(jsonObject.get(key_outputDirectory));
		
		} catch (IOException e) {
			
            e.printStackTrace();
            
		}
		
		System.out.println("\n\n------------- Debut du programme ----------------\n");

		
		System.out.println(jsonObject);
		
		System.out.println("This file contain " + jsonObject.size() + " objects");
		
		System.out.println("List of  objects ");
		
		System.out.println(jsonObject.keySet());	
		
		String  pageInfo = jsonObject.get("pageInfo").toString();
		 
		System.out.println("\n" + "pageName : " + pageInfo);
		 
		JSONObject pageNameObjectJson = (JSONObject) jsonObject.get("pageInfo");
		 
		String pagePic = pageNameObjectJson.get("pagePic").toString();
		 
		System.out.println("\n" + "pagePic : " + pagePic);
	
		String [] arrayPost = null;
				
		// System.out.println(jsonObject.get("mriqc_0.14.2.sif"));
		
		JSONObject objectJsonOfPipeline = null;	
		
		for (Object key : jsonObject.keySet()) {
			
			System.out.println(key);
			
			listKeysJsonFile.add((String) key);
			
			try {
				
					objectJsonOfPipeline = (JSONObject) jsonObject.get(key);
				
					getInfoJsonObject1(objectJsonOfPipeline);
					
					System.out.println("\n\n\n\n");
					
								
			} catch (Exception e) {
				
			}
			
		}
		
		
		System.out.println( listKeysJsonFile);
		
		System.out.println("\n---------------------------------\n");


    }


	 /* Cette méthode permet de récupérer un Object JSON  par sa clé */
	
    public static Object getJsonObjectByKey(JSONObject jsonObject, Object key) {
			
			return jsonObject.get(key);
			
    }

    /* 
     * Pour chaque objet JSON dans le fichier de configue
     * on récupére tout les  (key, value)     
     */
	
	public Map< String, String> getInfoJsonObject(JSONObject jsonObject){

		Map<String, String> jsonContent =  new HashMap<String, String>();
				
				for (Object key : jsonObject.keySet()) {
				
					jsonContent.put(key.toString(), jsonObject.get(key).toString());
					
				}
			
		return jsonContent;
	}


	public static void getInfoJsonObject1(JSONObject jsonObject){

		Map<String, String> jsonContent =  new HashMap<String, String>();
				
				for (Object key : jsonObject.keySet()) {
				
					jsonContent.put(key.toString(), jsonObject.get(key).toString());
					System.out.println("\"" + key.toString() + "\" : " + " \"" + jsonObject.get(key).toString() + "\"");
					
				}
	}


    public static String readFileAsString(String file)throws Exception {
        return new String(Files.readAllBytes(Paths.get(file)));
    
    }

    public static String  commandeDownloadData(String subjectSelected, String projectName, String dirIputdata, String lisSubjectWithSpaceSeparated){
        
        /* L'uri de XNAT */ 
        URI_HOST_XNAT = (String) jsonObject.get("URI_HOST_XNAT");

        String filenameTxt = "\"" + dirIputdata + "/download_commandLine.txt" +"\"";
        
        String filenameCsv = "\"" + dirIputdata + "/download_report.csv" +"\"";

        String dirDataInBIDS =  dirIputdata + "/" + projectName + "BIDS";
        
        String commande = "source activate dax_env\n"
                + "\n" + "filenameTxt=" + filenameTxt
                + "\n" + "filenameCsv=" + filenameCsv
                + "\n \n" + "mkdir -p " + dirIputdata
                + "\n" + "Xnatdownload "+ URI_HOST_XNAT + " -p " + projectName + " -d " + dirIputdata + " --subj ";

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
                    + "\n" +  "dirDataInBIDS=\"" + dirDataInBIDS + "\"\n"
                    + "\n" +  "python /envau/work/nit/xnat-cluster/xnat2bids_reconstruct_afterDownload.py " 
                    + dirIputdata + "/" + projectName + " " + "$dirDataInBIDS" +  " " + lisSubjectWithSpaceSeparated +"\n";
                    
            
          commande += "\nsource deactivate \n\n"; 

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

        if (whichPipeline.contains("fmriprep")) {

            return getCommandeFmriPrepSimg(whichPipeline, inputDirBIDS);

        }

        if (whichPipeline.contains("macapype")) {

            return getCommandeMacapype(whichPipeline, inputDirBIDS);
        }

        if (whichPipeline.contains("mriqc")) {

            return getCommandeMriqc(whichPipeline, inputDirBIDS);
        }

        if(whichPipeline.contains("bids_validator")){
            
            return getCommandeBidsValidator(whichPipeline,inputDirBIDS);
        }
        
        else return "";

    }


    // version : sera la commande passée
    public String  getCommandeMacapype(String version, String inputDirBIDS){
        
        String dirData = inputDirBIDS + "/" + ID_PROJECT + "BIDS";
        

        String commande = "\n" + "singularity run -B " + dirData + ":/data/macapype "
                        + " " + "-B " + inputDirBIDS + ":/out ";
        commande += "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " ";

        commande += "segment_pnh "
                  + "-data /data/macapype/ "
                  + "-out /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " "
                  + "-soft ANTS -species macaque ";
  
    /*     commande += "python /opt/packages/macapype/workflows/segment_pnh.py "
                  + "-data /data/macapype/ "
                  + "-out /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " "
                  + "-soft ANTS -params /opt/packages/macapype/workflows/params_segment_macaque_ants_based.json "; */
    
    return commande;
    }
    

    public String getCommandeFmriPrepSimg(String version , String inputDirBIDS){

        String dirdata = inputDirBIDS + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirdata + ":/work_dir "
                        + "-B " + inputDirBIDS + ":/output "
                        + "-B /hpc/shared/apps/x86_64/softs/freesurfer/7.1.1:/license_path "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "--fs-license-file /license_path/.license "
                        + "/work_dir" + " /output "
                        + "participant -w /output/temp_data_test/ "
                        + ""
                        + "--cifti-output --low-mem --mem-mb 32000 --nthreads 64";

                        /*
                        + "--fs-license-file /license_path/.license "
                        + "/workdir /workdir/derivatives/fmriprep "
                        + "participant --participant-label 01 -w /work_dir/temp_data_test/ "
                        + "--cifti-output --low-mem --mem-mb 32000 --nthreads 64";
                        */

        return commande; 
    }



    public String getCommandeMriqc(String version , String workDir){

        String dirData = workDir + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirData + ":/data "
                        + "-B " + workDir + ":/out "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "--verbose-reports "
                        + "/data /out/" + "output" + ID_PROJECT + "_" + datTimeNow + " participant "
                        + " group";
                  
        return commande;
         
    }


    /* BIDSApps bida_validator  */
    public String getCommandeBidsValidator(String version , String dataset){

        String dirData = dataset + "/" + ID_PROJECT + "BIDS";

        String commande = "\n" + "singularity run --cleanenv -B " + dirData + ":/data "
                        + "/hpc/shared/apps/x86_64/softs/singularity_BIDSApps/" + version + " "
                        + "/data"
                        + "\n";
                  
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






