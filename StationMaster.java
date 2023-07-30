import java.net.*;
import java.io.*;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.Base64; 
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.SecretKey;

public class StationMaster {
	private Socket socket;
	private ServerSocket server;
	private DataInputStream input, in;
	private DataOutputStream out;
	private static String endDelimiter = "$$";
	private final String secretKey = "THIS_IS_TOP_SECRET";
	private static String R1HOST = "127.0.0.1";
	private static int R1PORT = 3021;
	
	public StationMaster(int port){
		SHA2 sha = new SHA2();
		try{
			server = new ServerSocket(port);
			System.out.println("Listening on port:"+port);
			socket = server.accept();
			System.out.println("Secure connection established from Train");
			input = new DataInputStream(System.in);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
			String rhash = "";
			String chash = "";
			String msg = "";
			String line = "";
			String response = "";
			try{
				line = in.readUTF();
				msg = line.split("\n")[0];
				rhash = line.split("\n")[1];
				System.out.println("Received Train Time: " +msg.split(" ")[0]);
				System.out.println("Received Compartment Sequence: " +msg.split(" ")[1]);
				System.out.println("Received Hash of message: " +rhash);
				chash = sha.SHA256(msg);
				if(msg.equals("$$")){
					out.writeUTF("bye");
				}
				System.out.println("Computed Hash: "+chash);
				String ack = "";
				if(rhash.equals(chash)){
					System.out.println("Message Integrity is verified, checking platform availability...");
					System.out.println("Platform No. 2 available");
					ack = "2";
					System.out.print("Platform Number 2 is sent to Train\n");
					out.writeUTF(ack);
				forwardMessage(R1HOST, R1PORT, msg);
				}else{
					System.out.println("Message Integrity check failed, aborting communication");
					ack = "Message integrity failed\n"+line;
					System.out.print("Acknowledgement Sent to Source\n");
					out.writeUTF(ack);
				}
			}catch(IOException e){
				e.printStackTrace();
				return;
			}
			System.out.println("Closing connection");
			socket.close();
			in.close();
		}catch(IOException e){
			e.printStackTrace();
			return;
		}
	}
	
	public void forwardMessage(String ipAddress, int port, String message){
		SHA2 sha = new SHA2();
		DES aes = new DES();
		try{
			socket = new Socket(ipAddress, port);
			System.out.println("\nSecure connection is established to Platform");
			input = new DataInputStream(System.in);
			out = new DataOutputStream(socket.getOutputStream());
			in = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
		}catch(Exception e){
			System.out.println("Unable to connect to Platforms");
			return;
		}
		String response = "";
		String msg = "";
		String enc = "";
		try{
			msg = message;
			enc = aes.encrypt(msg, secretKey);
			System.out.println("\nEncrypted Compartment Sequence Sent to Platform :"+enc);
			out.writeUTF(enc);
			response = in.readUTF();
			System.out.println("Response from Platform: "+response);
		}catch(IOException e){
			e.printStackTrace();
		}
	}
	
	public static void main(String args[]){
		StationMaster server = new StationMaster(3020);
	}
}

class DES {
	private static SecretKey secretKey;
	private static byte[] key;
	
	public static void setKey(String keyString){
		MessageDigest sha = null;
		try{
			DESKeySpec dks = new DESKeySpec(keyString.getBytes());
			SecretKeyFactory skf = 			SecretKeyFactory.getInstance("DES");
			secretKey = skf.generateSecret(dks);
		}catch(Exception e){
			System.out.println("Exception occured while generating key");
		}
	}
	
	public static String encrypt(String message, String key){
		try{
			setKey(key);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			return Base64.getEncoder().encodeToString(cipher.doFinal(message.getBytes("UTF-8")));
		}catch(Exception e){
			System.out.println("Exception occured while encrypting");
		}
		return null;
	}
	
	public static String decrypt(String ciphertext, String key){
		try{
			setKey(key);
			Cipher cipher = Cipher.getInstance("DES");
			cipher.init(Cipher.DECRYPT_MODE,secretKey);
			return new String(cipher.doFinal(Base64.getDecoder().decode(ciphertext)));
		}catch(Exception e){
			System.out.println("Exception occured while encrypting");
		}
		return null;
	}
}

class SHA2 {
    static String SHA256(String input){
        StringBuilder bInput = new StringBuilder();
        int bigEndian = 0;
        for(int i=0;i<input.length();i++){
            StringBuilder sb = new StringBuilder(Integer.toBinaryString(input.charAt(i)));
            while(sb.length() < 8){
                sb.insert(0,"0");
            }
            bInput.append(sb.toString());
        }
        bigEndian = bInput.length();
        bInput.append("1");
        while(bInput.length() % 512 != 0){
            bInput.append("0");
        }
        bInput = new StringBuilder(bInput.substring(0,bInput.length()-64));
        StringBuilder sb = new StringBuilder(Integer.toBinaryString(bigEndian));
        while(sb.length() < 64){
            sb.insert(0,"0");
        }
        bInput.append(sb.toString());
        
        StringBuilder a,b,c,d,e,f,g,h;
        a = new StringBuilder();
        b = new StringBuilder();
        c = new StringBuilder();
        d = new StringBuilder();
        e = new StringBuilder();
        f = new StringBuilder();
        g = new StringBuilder();
        h = new StringBuilder();
        int h0,h1,h2,h3,h4,h5,h6,h7;
        h0 = 0x6a09e667;
        h1 = 0xbb67ae85;
        h2 = 0x3c6ef372;
        h3 = 0xa54ff53a;
        h4 = 0x510e527f;
        h5 = 0x9b05688c;
        h6 = 0x1f83d9ab;
        h7 = 0x5be0cd19;
        int round[] = new int[] { 0x428a2f98, 0x71374491, 0xb5c0fbcf, 0xe9b5dba5, 0x3956c25b, 0x59f111f1, 0x923f82a4, 0xab1c5ed5,
0xd807aa98, 0x12835b01, 0x243185be, 0x550c7dc3, 0x72be5d74, 0x80deb1fe, 0x9bdc06a7, 0xc19bf174,
0xe49b69c1, 0xefbe4786, 0x0fc19dc6, 0x240ca1cc, 0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
0x983e5152, 0xa831c66d, 0xb00327c8, 0xbf597fc7, 0xc6e00bf3, 0xd5a79147, 0x06ca6351, 0x14292967,
0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13, 0x650a7354, 0x766a0abb, 0x81c2c92e, 0x92722c85,
0xa2bfe8a1, 0xa81a664b, 0xc24b8b70, 0xc76c51a3, 0xd192e819, 0xd6990624, 0xf40e3585, 0x106aa070,
0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5, 0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
0x748f82ee, 0x78a5636f, 0x84c87814, 0x8cc70208, 0x90befffa, 0xa4506ceb, 0xbef9a3f7, 0xc67178f2 };
        StringBuilder chunks[] = new StringBuilder[bInput.length()/512];
        int c1 = 0;
        for(int i=0;i<bInput.length()/512;i++){
            chunks[i] = new StringBuilder(bInput.substring(0+c1,512+c1));
            c1 += 512;
            StringBuilder W[] = new StringBuilder[64];
            int c2 = 0;
            for(int j=0;j<Math.ceil((double)bigEndian/32);j++){
                W[j] = new StringBuilder(bInput.substring(0+c2,32+c2));
                c2 += 32;
            }
            for(int j=(int)Math.ceil((double)bigEndian/32); j<64; j++){
                W[j] = new StringBuilder("00000000000000000000000000000000");
            }
            for(int j=16;j<64;j++){
                StringBuilder s0 = xor(xor(rightRotate(W[j-15],7),rightRotate(W[j-15],18)),rightShift(W[j-15],3));
                StringBuilder s1 = xor(xor(rightRotate(W[j-2],17),rightRotate(W[j-2],19)),rightShift(W[j-2],10));
                W[j] = add(add(add(W[j-16], s0),W[j-7]),s1);
            }
            a = new StringBuilder(Integer.toBinaryString(h0));
            while(a.length() < 32){
                a.insert(0,"0");
            }
            b = new StringBuilder(Integer.toBinaryString(h1));
            while(b.length() < 32){
                b.insert(0,"0");
            }
            c = new StringBuilder(Integer.toBinaryString(h2));
            while(c.length() < 32){
                c.insert(0,"0");
            }
            d = new StringBuilder(Integer.toBinaryString(h3));
            while(d.length() < 32){
                d.insert(0,"0");
            }
            e = new StringBuilder(Integer.toBinaryString(h4));
            while(e.length() < 32){
                e.insert(0,"0");
            }
            f = new StringBuilder(Integer.toBinaryString(h5));
            while(f.length() < 32){
                f.insert(0,"0");
            }
            g = new StringBuilder(Integer.toBinaryString(h6));
            while(g.length() < 32){
                g.insert(0,"0");
            }
            h = new StringBuilder(Integer.toBinaryString(h7));
            while(h.length() < 32){
                h.insert(0,"0");
            }
            for(int j=0;j<64;j++){
                StringBuilder s1 = xor(xor(rightRotate(e,6), rightRotate(e,11)), rightRotate(e,25));
                StringBuilder ch = xor(and(e,f),and(not(e), g));
                StringBuilder k = new StringBuilder(Integer.toBinaryString(round[j]));
                while(k.length() < 32){
                    k.insert(0,"0");
                }
                StringBuilder temp1 = add(add(add(add(h,s1),ch),k),W[j]);
                StringBuilder s0 = xor(xor(rightRotate(a,2), rightRotate(a,13)), rightRotate(a,22));
                StringBuilder maj = xor(xor(and(a,b),and(a,c)),and(b,c));
                StringBuilder temp2 = add(s0,maj);
                h = g;
                g = f;
                f = e;
                e = add(d,temp1);
                d = c;
                c = b;
                b = a;
                a = add(temp1, temp2);
            }
        }
        StringBuilder aa,bb,cc,dd,ee,ff,gg,hh;
        aa = new StringBuilder(Integer.toBinaryString(h0));
        while(aa.length() < 32){
            aa.insert(0,"0");
        }
        bb = new StringBuilder(Integer.toBinaryString(h1));
        while(bb.length() < 32){
            bb.insert(0,"0");
        }
        cc = new StringBuilder(Integer.toBinaryString(h2));
        while(cc.length() < 32){
            cc.insert(0,"0");
        }
        dd = new StringBuilder(Integer.toBinaryString(h3));
        while(dd.length() < 32){
            dd.insert(0,"0");
        }
        ee = new StringBuilder(Integer.toBinaryString(h4));
        while(ee.length() < 32){
            ee.insert(0,"0");
        }
        ff = new StringBuilder(Integer.toBinaryString(h5));
        while(ff.length() < 32){
            ff.insert(0,"0");
        }
        gg = new StringBuilder(Integer.toBinaryString(h6));
        while(gg.length() < 32){
            gg.insert(0,"0");
        }
        hh = new StringBuilder(Integer.toBinaryString(h7));
        while(hh.length() < 32){
            hh.insert(0,"0");
        }
        String A = Long.toString(Long.parseLong(add(aa,a).toString(),2),16);
        String B = Long.toString(Long.parseLong(add(bb,b).toString(),2),16);
        String C = Long.toString(Long.parseLong(add(cc,c).toString(),2),16);
        String D = Long.toString(Long.parseLong(add(dd,d).toString(),2),16);
        String E = Long.toString(Long.parseLong(add(ee,e).toString(),2),16);
        String F = Long.toString(Long.parseLong(add(ff,f).toString(),2),16);
        String G = Long.toString(Long.parseLong(add(gg,g).toString(),2),16);
        String H = Long.toString(Long.parseLong(add(hh,h).toString(),2),16);
        String digest = A + B + C + D + E + F + G + H;
        return digest;
    }
    
    static StringBuilder rightRotate(StringBuilder sb, int rot){
        for(int i=0;i<rot;i++){
            sb.insert(0,sb.charAt(sb.length()-1));
            sb.deleteCharAt(sb.length()-1);
        }
        return sb;
    }
    
    static StringBuilder rightShift(StringBuilder sb, int sh){
        for(int i=0;i<sh;i++){
            sb.insert(0,"0");
            sb.deleteCharAt(sb.length()-1);
        }
        return sb;
    }
    
    static StringBuilder xor(StringBuilder a, StringBuilder b){
        StringBuilder c = new StringBuilder();
        for(int i=0;i<a.length();i++){
            if(a.charAt(i) == b.charAt(i)){
                c.insert(i,"0");   
            }else{
                c.insert(i,"1");
            }
        }
        return c;
    }
    
    static StringBuilder add(StringBuilder a, StringBuilder b){
        StringBuilder c = new StringBuilder();
        char carry = '0';
        for(int i=a.length()-1; i >= 0; i--){
            if(carry == '0'){
                if(a.charAt(i)=='0' && b.charAt(i) == '0'){
                    c.insert(0,"0");
                }else if(a.charAt(i)=='0' && b.charAt(i) == '1'){
                    c.insert(0,"1");
                }else if(a.charAt(i)=='1' && b.charAt(i) == '0'){
                    c.insert(0,"1");
                }else if(a.charAt(i)=='1' && b.charAt(i) == '1'){
                    c.insert(0,"0");
                    carry = '1';
                }
            }else{
                if(a.charAt(i)=='0' && b.charAt(i) == '0'){
                    c.insert(0,"1");
                    carry = '0';
                }else if(a.charAt(i)=='0' && b.charAt(i) == '1'){
                    c.insert(0,"0");
                }else if(a.charAt(i)=='1' && b.charAt(i) == '0'){
                    c.insert(0,"0");
                }else if(a.charAt(i)=='1' && b.charAt(i) == '1'){
                    c.insert(0,"1");
                }
            }
        }
        return c;
    }
    
    static StringBuilder and(StringBuilder a, StringBuilder b){
        StringBuilder c = new StringBuilder();
        for(int i=0;i<a.length();i++){
            if(a.charAt(i)=='1' && b.charAt(i)=='1'){
                c.insert(i,"1");
            }else{
                c.insert(i,"0");
            }
        }
        return c;
    }
    
    static StringBuilder not(StringBuilder a){
        StringBuilder b = new StringBuilder();
        for(int i=0;i<a.length();i++){
            if(a.charAt(i)=='0'){
                b.insert(i,"1");
            }else{
                b.insert(i,"0");
            }
        }
        return b;
    }
}
