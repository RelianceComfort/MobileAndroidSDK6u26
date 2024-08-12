package com.metrix.architecture.utilities;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import android.util.Base64;

/**
 * the helper class to compute Sha512 hash using US-ASCII format.  
 */
public class MetrixSecurityHelper {
	// public static int PW_HASH_ITERATION_COUNT = 5000;
	private static MessageDigest md;

	public static String HashPassword(String password) {
		String salt = "";

		String result = computeSha512Hash(password, salt);
		return result.replace("\n", "");
	}
	
	public static String HashPassword(String password, String salt) {
		String result = computeSha512Hash(password, salt);
		return result.replace("\n", "");
	}	
	
	/**
	 * @param input - a input string to be hashed
	 * @param salt - seed for hash computation
	 * @return - a string that is generated as hash string 
	 */
	private static String computeSha512Hash(String input, String salt) {
		byte[] bSaltOriginal;
		byte[] bPasswordOriginal;
		byte[] bPassword;
		byte[] bSalt;

		try {
			md = MessageDigest.getInstance("SHA-512");

			bSaltOriginal = salt.getBytes("US-ASCII");
			bPasswordOriginal = input.getBytes("US-ASCII");
			bPassword = new byte[bPasswordOriginal.length * 2];
			bSalt = new byte[bSaltOriginal.length * 2];
			
			for (int i = 0; i < (bPasswordOriginal.length * 2 - 1); i++) {
				if (i % 2 == 0) {
					bPassword[i] = bPasswordOriginal[i / 2];
				} else {
					bPassword[i] = 0;
				}
			}
			
			for (int i = 0; i < (bSaltOriginal.length * 2 - 1); i++) {
				if (i % 2 == 0) {
					bSalt[i] = bSaltOriginal[i / 2];
				} else {
					bSalt[i] = 0;
				}
			}
			
		} catch (NoSuchAlgorithmException e) {
			LogManager.getInstance().error(e);
			throw new RuntimeException(AndroidResourceHelper.getMessage("NoSuchAlgorithm"));
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(AndroidResourceHelper.getMessage("UnsupportedEncoding"), e);
		}

		byte[] digest = run(bPassword, bSalt);
		// 0 is default flag for encoding the string
		return Base64.encodeToString(digest, 0);
	}

	/**
	 * @param input - the byte array to be computed 
	 * @param salt - the seed for the computation
	 * @return the hashed byte array
	 */
	private static byte[] run(byte[] input, byte[] salt) {
		md.update(input);
		return md.digest();
		//return md.digest(salt);		
	}
}