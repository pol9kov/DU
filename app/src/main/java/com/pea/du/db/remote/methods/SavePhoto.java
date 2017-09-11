package com.pea.du.db.remote.methods;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import com.jcraft.jsch.*;
import com.pea.du.R;
import com.pea.du.actyvities.addresses.works.WorksActivity;
import com.pea.du.actyvities.addresses.works.defectation.DefectActivity;
import com.pea.du.data.Act;
import com.pea.du.data.Photo;
import com.pea.du.db.local.methods.WriteMethods;

import java.io.File;
import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import static com.pea.du.db.remote.MysqlConnection.getConnection;
import static com.pea.du.flags.Flags.actId;
import static com.pea.du.flags.Flags.addressId;
import static com.pea.du.flags.Flags.workId;

/*
        Сохраняет в MySQL, и сохраняет в локальную бд. Хранит объект private photo.
 */
public class SavePhoto extends AsyncTask<String,String,String> {

    Context context = null;
    String z = "";
    Boolean isSuccess = false;
    Connection con;


    private Photo photo = new Photo();

    public SavePhoto(Context context, Photo photo){
        this.context = context;
        this.photo = photo;
    }

    @Override
    protected void onPreExecute()
    {
        Toast.makeText(context , "Сохранение фото..." , Toast.LENGTH_LONG).show();
    }

    @Override
    protected void onPostExecute(String r)
    {

        Toast.makeText(context, r, Toast.LENGTH_SHORT).show();
        Button bAddPhoto = (Button) ((DefectActivity) context).findViewById(R.id.activity_defect_addPhoto);
        bAddPhoto.setEnabled(true);
    }

    @Override
    protected String doInBackground(String... params)
    {
        try
        {
            con = getConnection();        // Connect to database
            if (con == null)
            {
                z = "Проверьте своё подключение к интернету!";
            }
            else
            {
                Integer newID = 0;
                String query = "SELECT MAX(id) from zkh_actofdefectphoto";
                Statement stmt = con.createStatement();
                ResultSet rs = stmt.executeQuery(query);
                if(rs.next()){ newID = (rs.getInt(1)) + 1;}

                query = "INSERT INTO zkh_actofdefectphoto (Header, DefectFileLink) " +
                        "VALUES (" + workId + ",'http://78.47.195.208/MSK3/DeffectAct/" + workId + "/" + newID +".jpg')";
                stmt.executeUpdate(query);
                photo.setServerId(newID);

                sendSSHPhoto();

            }
        }
        catch (Exception ex)
        {
            isSuccess = false;
            z = ex.getMessage();
        }
        return z;
    }

    private void sendSSHPhoto(){
        String SFTPHOST = "78.47.195.208";
        int SFTPPORT = 22;
        String SFTPUSER = "root";
        String SFTPPASS = "he7TRt2fFwycUD";
        String SFTPWORKINGDIR = "/var/www/html/MSK3/DeffectAct";

        Session session = null;
        Channel channel = null;
        ChannelSftp channelSftp = null;
        Log.println(1, "SSH1: ", "preparing the host information for sftp.");
        try {
            JSch jsch = new JSch();
            session = jsch.getSession(SFTPUSER, SFTPHOST, SFTPPORT);
            session.setPassword(SFTPPASS);
            java.util.Properties config = new java.util.Properties();
            config.put("StrictHostKeyChecking", "no");
            session.setConfig(config);
            session.connect();
            Log.println(1, "SSH2: ", "Host connected.");
            channel = session.openChannel("sftp");
            channel.connect();
            Log.println(1, "SSH3: ", "sftp channel opened and connected.");
            channelSftp = (ChannelSftp) channel;

            try {
                channelSftp.cd( SFTPWORKINGDIR + "/" + workId );
            }
            catch ( SftpException e ) {
                channelSftp.mkdir( SFTPWORKINGDIR + "/" + workId );
                channelSftp.cd( SFTPWORKINGDIR + "/" + workId );
            }

            File f = new File(photo.getPath());
            channelSftp.put(new FileInputStream(f), photo.getServerId().toString()+".jpg");
            z = "Фото сохранено на сервер";

        } catch (Exception ex) {
            isSuccess = false;
            z = ex.getMessage();
        }
        finally{

            channelSftp.exit();
            Log.println(1, "SSH4: ", "sftp Channel exited.");
            channel.disconnect();
            Log.println(1, "SSH5: ", "Channel disconnected.");
            session.disconnect();
            Log.println(1, "SSH6: ", "Host Session disconnected.");

        }

    }


}