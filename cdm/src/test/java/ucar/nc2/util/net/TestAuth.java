/*
 * Copyright 1998-2009 University Corporation for Atmospheric Research/Unidata
 *
 * Portions of this software were developed by the Unidata Program at the
 * University Corporation for Atmospheric Research.
 *
 * Access and use of this software shall impose the following obligations
 * and understandings on the user. The user is granted the right, without
 * any fee or cost, to use, copy, modify, alter, enhance and distribute
 * this software, and any derivative works thereof, and its supporting
 * documentation for any purpose whatsoever, provided that this entire
 * notice appears in all copies of the software, derivative works and
 * supporting documentation.  Further, UCAR requests that the user credit
 * UCAR/Unidata in any publications that result from the use of this
 * software or in any product that includes this software. The names UCAR
 * and/or Unidata, however, may not be used in any advertising or publicity
 * to endorse or promote any products or commercial entity unless specific
 * written permission is obtained from UCAR/Unidata. The user also
 * understands that UCAR/Unidata is not obligated to provide the user with
 * any support, consulting, training or assistance of any kind with regard
 * to the use, operation and performance of this software nor to provide
 * the user with any updates, revisions, new versions or "bug fixes."
 *
 * THIS SOFTWARE IS PROVIDED BY UCAR/UNIDATA "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL UCAR/UNIDATA BE LIABLE FOR ANY SPECIAL,
 * INDIRECT OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES WHATSOEVER RESULTING
 * FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN ACTION OF CONTRACT,
 * NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR IN CONNECTION
 * WITH THE ACCESS, USE OR PERFORMANCE OF THIS SOFTWARE.
 */

package ucar.nc2.util.net;

import junit.framework.TestCase;

import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.CredentialsProvider;
import org.apache.commons.httpclient.auth.CredentialsNotAvailableException;
import org.apache.commons.httpclient.auth.AuthScheme;
import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.protocol.Protocol;
import org.junit.Test;

import java.io.*;
import java.util.List;
import java.io.Serializable;

public class TestAuth extends ucar.nc2.util.TestCommon
{
    // Add a temporary control for motherlode versus localhost
    static boolean motherlode = false;

    // TODO: add proxy and digest tests

    // Assuming we have thredds root, then the needed keystores
    // are located in this directory
    static final String KEYDIR = "/cdm/src/test/resources";

    static final String CLIENTKEY = "clientkey.jks";
    static final String CLIENTPWD = "changeit";

    // Mnemonics for xfail
    static final boolean MUSTFAIL = true;
    static final boolean MUSTPASS = false;

    static {
    // Register the 8843 protocol to test client side keys
    HTTPSession.registerProtocol("https", 8843,
                     new Protocol("https",
                                  new EasySSLProtocolSocketFactory(),
                                  8843));
    }

    //////////////////////////////////////////////////
    // Provide a non-interactive CredentialsProvider to hold
    // the user+pwd; used in several places
    static class BasicProvider implements CredentialsProvider, Serializable
    {
        String username = null; String password = null;

        public BasicProvider(String username, String password)
        {
	    this.username = username;
	    this.password = password;
        }     

	// Credentials Provider Interface
        public Credentials
        getCredentials(AuthScheme authscheme, String host, int port, boolean isproxy)
            throws CredentialsNotAvailableException
        {
            UsernamePasswordCredentials creds = new UsernamePasswordCredentials(username,password);
            System.out.printf("getCredentials called: creds=|%s| host=%s port=%d isproxy=%b authscheme=%s%n",
                      creds.toString(),host,port,isproxy,authscheme);
             return creds;
        }

        // Serializable Interface
        private void writeObject(java.io.ObjectOutputStream oos)
            throws IOException
        {oos.writeObject(this.username);oos.writeObject(this.password);}

        private void readObject(java.io.ObjectInputStream ois)
            throws IOException, ClassNotFoundException
        {
	    this.username = (String)ois.readObject();
            this.password = (String)ois.readObject();
        }
    }

    //////////////////////////////////////////////////
    //////////////////////////////////////////////////

    // Define the test sets

    int passcount = 0;
    int xfailcount = 0;
    int failcount = 0;
    boolean verbose = true;
    boolean pass = false;

    String datadir = null;
    String threddsroot = null;

    public TestAuth(String name, String testdir)
    {
        super(name);
	setTitle("DAP Authorization tests");
    }

    public TestAuth(String name)
    {
        this(name, null);
    }


    //@Test
    public void
    xtestSSH() throws Exception
    {
        String[] sshurls = {
            "https://motherlode.ucar.edu:8443/dts/b31.dds"
        };

        System.out.println("*** Testing: Simple Https");
        for(String url: sshurls) {
            System.out.println("*** URL: " + url);
            HTTPSession session = new HTTPSession(url);
            HTTPMethod method = HTTPMethod.Get(session);
            int status = method.execute();
            System.out.printf("\tstatus code = %d\n", status);
            pass = (status == 200);
            junit.framework.Assert.assertTrue("testSSH", pass);
        }
    }

    //////////////////////////////////////////////////

    static class AuthDataBasic
    {
        String url;
        String user = null;
        String password = null;
        boolean xfail = false; // failure expected
        public AuthDataBasic(String url, String usr, String pwd, boolean xfail)
        {
            this.url = url;
            this.user = usr;
            this.password = pwd;
            this.xfail = xfail;
        }
    }

    //@Test
    public void
    xtestBasic() throws Exception
    {
        AuthDataBasic[] basictests = {
            new AuthDataBasic("http://motherlode.ucar.edu:8080/thredds/dodsC/restrict/testdata/testData.nc.html",
                              "tiggeUser","tigge", MUSTPASS),
            new AuthDataBasic("http://motherlode.ucar.edu:8080/thredds/dodsC/restrict/testdata/testData.nc.html",
                              "","", MUSTFAIL)
        };

        System.out.println("*** Testing: Http Basic Password Authorization");

        for(AuthDataBasic data: basictests) {
            CredentialsProvider provider = new BasicProvider(data.user, data.password);
            System.out.println("*** URL: " + data.url);
            // Test local credentials provider
            HTTPSession session = new HTTPSession(data.url);
            session.setCredentialsProvider(provider);
            HTTPMethod method = HTTPMethod.Get(session);
            int status = method.execute();
            System.out.printf("\tlocal provider: status code = %d\n", status);  System.out.flush();
            session.setCredentialsProvider(null);
            pass = (status == 200 || status == 404); // non-existence is ok
            String msg;
            if(data.xfail) {
                msg = pass ? "Local test failed to fail (xfail)" : "Local test passed (xfail)";
                pass = !pass;
            } else {
                msg = pass ? "Local test passed" : "Local test failed";
            }
            System.out.println("\t"+msg);
            if(pass) {
                // Test global credentials provider
                HTTPSession.setGlobalCredentialsProvider(provider);
                session = new HTTPSession(data.url);
                method = HTTPMethod.Get(session);
                status = method.execute();
                System.out.printf("\tglobal provider test: status code = %d\n", status);  System.out.flush();
                pass = (status == 200 || status == 404); // non-existence is ok
                if(data.xfail) {
                    msg = pass ? "Local test failed to fail (xfail)" : "Local test passed (xfail)";
                    pass = !pass;
                } else {
                    msg = pass ? "Local test passed" : "Local test failed";
                }
                System.out.println("\t"+msg);
            }
            if(pass)
                junit.framework.Assert.assertTrue("testBasic", true);
            else
                junit.framework.Assert.assertTrue("testBasic", false);
        }
    }

    @Test
    public void
    testKeystore() throws Exception
    {
        System.out.println("*** Testing: Client-side Key based Authorization");

        String server;
        String path;
        if(motherlode) {
            server = "motherlode.ucar.edu:8843";
            path = "/dts/b31.dds";
        } else {
            server = "localhost:8843";
            path = "/thredds/dodsC/testStandardTdsScan/1day.nc.dds";
        }

        String url = "https://"+server+path;
        System.out.println("*** URL: " + url);

        // See if the client keystore exists
        String keystore = threddsRoot + KEYDIR + "/" + CLIENTKEY;
        File tmp = new File(keystore);
        if(!tmp.exists() || !tmp.canRead())
            throw new Exception("Cannot read client key store: "+keystore);

        CredentialsProvider provider = new HTTPSSLProvider(keystore,CLIENTPWD);
        HTTPSession.setGlobalCredentialsProvider(HTTPAuthScheme.SSL,provider);

        HTTPSession session = new HTTPSession(url);

        //session.setCredentialsProvider(provider);

        HTTPMethod method = HTTPMethod.Get(session);

        int status = method.execute();
        System.out.printf("Execute: status code = %d\n", status);
        pass = (status == 200);
        if(pass)
                junit.framework.Assert.assertTrue("testKeystore", true);
            else
                junit.framework.Assert.assertTrue("testKeystore", false);
        }

        //@Test
        public void
        xtestSerialize() throws Exception
        {
            System.out.println("*** Testing: HTTPAuthStore (de-)serialization");

            boolean ok = true;
            CredentialsProvider creds1 = new BasicProvider("p1","pwd");
            CredentialsProvider creds2 = new HTTPSSLProvider("keystore","keystorepwd");
            CredentialsProvider creds3 = new BasicProvider("p3","pwd3");

            // Add some entries to HTTPAuthStore
            HTTPAuthStore.clear();
            HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                    HTTPAuthScheme.BASIC,
                    "http://ceda.ac.uk/dap/neodc/casix/seawifs_plankton/data/monthly/PSC_monthly_1998.nc.dds",
                    creds1)
            );
            HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                    HTTPAuthScheme.SSL,
                    "http://ceda.ac.uk",
                    creds2)
            );
            HTTPAuthStore.insert(new HTTPAuthStore.Entry(
                    HTTPAuthScheme.BASIC,
                    "http://ceda.ac.uk",
                    creds3)
            );

            // Remove any old file
            File target1 = new File("./serial1");
            target1.delete();

            // serialize out
            OutputStream ostream = new FileOutputStream(target1);
            HTTPAuthStore.serialize(ostream, "password1");
            // Read in
            InputStream istream = new FileInputStream(target1);
            List<HTTPAuthStore.Entry> entries = HTTPAuthStore.getDeserializedEntries(istream, "password1");

            // compare
            List<HTTPAuthStore.Entry> rows = HTTPAuthStore.getAllRows();
            for (HTTPAuthStore.Entry row : rows) {
                HTTPAuthStore.Entry match = null;
                for (HTTPAuthStore.Entry e : entries) {
            block:          {
                    if (!HTTPAuthStore.Entry.identical(row,e)) break block;
                    if(match == null)
                        match = e;
                    else {System.out.println("ambigous match");  ok=false;}
                    }
                }
                if (match == null) {
                    System.out.println("no match for: " + row.toString());  ok=false;
            }
        }
        junit.framework.Assert.assertTrue("test(De-)Serialize", ok);
    }


}
