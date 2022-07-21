

package org.nrg.xnat.icm.rest;

import java.util.Arrays;
import org.slf4j.LoggerFactory;
import org.nrg.xnat.icm.plugin.XnatIcmExportDataPlugin;
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

@Api(description = "")
@XapiRestController
@RequestMapping({ "/export-data" })
public class XnatIcmExportDataApi
{
    private static final Logger _logger;
    private static List<String> _xnatSessionTypes;
    private static String _dcm2niixExecutablePath;
    private String _tempBuildPath;
    private boolean _imageFormatAddDicoms;
    private boolean _imageFormatAddNiftis;
    private boolean _imageFormatAddBothImageFormats;
    
    @ApiOperation(value = "Exports files under a certain format", notes = "Custom")
    @ApiResponses({ @ApiResponse(code = 200, message = "Export successfully done"), @ApiResponse(code = 401, message = "Must be authenticated to access the XNAT Rest Api"), @ApiResponse(code = 500, message = "Unexpected internal serval error") })
    @RequestMapping(value = { "/export-files/{id_project}" }, produces = { "application/zip" }, method = { RequestMethod.POST })
    @ResponseBody
    public void exportFiles(final HttpServletResponse response, @PathVariable final String id_project, @RequestParam("subject_ids") final String subject_ids, @RequestParam("image_format_choice") final String image_format_choice, @RequestParam("disk_tree_format_choice") final String disk_tree_format_choice) {
        final UserI xnatUser = XDAT.getUserDetails();
        ByteArrayOutputStream baos = null;
        final List<String> subjectsList = new LinkedList<String>();
        log("Exporting from project [" + id_project + "]...");
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
        XnatIcmExportDataApi._logger.info(logString);
    }
    
    static {
        _logger = LoggerFactory.getLogger((Class)XnatIcmExportDataPlugin.class);
        XnatIcmExportDataApi._xnatSessionTypes = Arrays.asList("xnat:mrSessionData", "xnat:crSessionData", "xnat:ctSessionData", "xnat:petSessionData", "xnat:petmrSessionData");
        XnatIcmExportDataApi._dcm2niixExecutablePath = "dcm2niix";
    }
}