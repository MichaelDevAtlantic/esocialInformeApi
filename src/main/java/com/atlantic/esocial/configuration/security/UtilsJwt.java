package com.atlantic.esocial.configuration.security;

import java.security.Key;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.spec.SecretKeySpec;
import jakarta.xml.bind.DatatypeConverter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

/**
 * Classe para tratar da integracao JWT entre sistemas
 * @author Tony Caravana Campos
 * @version 1.00
 */
public class UtilsJwt {
	
    private final static String COD_NUM_INT_JWT_RECAD_BENF              = "1";
    public  final static String COD_NUM_INT_JWT_NOVAPREV                = "5"; //4 //5: Novaprev
    public  final static String COD_NUM_INT_JWT_MODULO_SGRH_FUNCIONAL   = "6";
    
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_ISSUER     = "INT_JWT_AUTH_ISSUER";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_SUBJECT    = "INT_JWT_AUTH_SUBJECT";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_EXPIRATION = "INT_JWT_AUTH_EXPIRATION";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_ALGORITHM  = "INT_JWT_AUTH_ALGORITHM";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY = "INT_JWT_AUTH_SECRET_KEY";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_URL        = "INT_JWT_AUTH_URL";
    private final static String COD_NUM_INT_JWT_COD_PAR_AUTH_REDIRECT   = "INT_JWT_AUTH_ALLOW_REDIRECT";

    private static Map<String, String> getJWTParams() throws Exception {
    	return getJWTParams(COD_NUM_INT_JWT_RECAD_BENF);
    }
    
    private static Map<String, String> getJWTParams(String _COD_NUM) throws Exception {

    	Map<String, String>         map = new HashMap<String, String>();
    	
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_ISSUER,     "alesp-funcional");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_SUBJECT,    "jwt-auth");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_EXPIRATION, "");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_ALGORITHM,  "HS256");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY, "a239824573331ee372214593ce2f3b23871452d141cdc13922faaade8fc21cf11c883cfe111f06bae287123b5132a8e0deafc02cf73acfebb9771f9cf261f3fd");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_URL,        "");
		map.put(COD_NUM_INT_JWT_COD_PAR_AUTH_REDIRECT,   "YES");
    	
    	return map;
    }   
    
    public static String generateJWTToken(String userId) throws Exception {

    	Map<String, String> configMap = getJWTParams();    	
    	
		//The JWT signature algorithm we will be using to sign the token
    	SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		//We will sign our JWT with our ApiKey secret
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY));
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());

		//Let's set the JWT Claims
		 String data =  ("{ \"codIdeCli\":  \""+userId+"\" }");

		JwtBuilder builder = Jwts.builder().setId("1L")
				.setIssuedAt(now)
				.setSubject (configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SUBJECT))
				.setIssuer  (configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_ISSUER))
				.claim      ("data", data)
				.signWith   (signatureAlgorithm, signingKey);
		
		
		String ttlMillisStr = configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_EXPIRATION); 
		long ttlMillis = (!ttlMillisStr.replaceAll("^[0-9]", "").equals("") ? new Long(ttlMillisStr.replaceAll("^[0-9]", "")) : -1);
		
		//if it has been specified, let's add the expiration
		if (ttlMillis >= 0) {
            long expMillis = nowMillis + ttlMillis;
            Date exp = new Date(expMillis);
            builder.setExpiration(exp);
        }
		
		//Builds the JWT and serializes it to a compact, URL-safe string
		return builder.compact();
	}   
    
    public static String generateJWTToken(String codNumProjeto, String dataJson) throws Exception {
		Map<String, String> configMap = getJWTParams(codNumProjeto);  
	
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
	
		long nowMillis = System.currentTimeMillis();
		Date now = new Date(nowMillis);
		byte[] apiKeySecretBytes = DatatypeConverter.parseBase64Binary(configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY));
		Key signingKey = new SecretKeySpec(apiKeySecretBytes, signatureAlgorithm.getJcaName());
	
		JwtBuilder builder = Jwts.builder().setId("1L")
			.setIssuedAt(now)
			.setSubject (configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SUBJECT))
			.setIssuer  (configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_ISSUER))
			.claim      ("data", dataJson)
			.signWith   (signatureAlgorithm, signingKey);
		        //.setExpiration(dateexp)
	
	//	String ttlMillisStr = configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_EXPIRATION); 
	//	long ttlMillis = (!Utils.trim(ttlMillisStr).replaceAll("^[0-9]", "").equals("") ? new Long(Utils.trim(ttlMillisStr).replaceAll("^[0-9]", "")) : -1);
	//
	//	if (ttlMillis >= 0) {
	//	    long expMillis = nowMillis + ttlMillis;
	//	    Date exp = new Date(expMillis);
	//	    //builder.setExpiration(exp);
	//	}
				
		return builder.compact();
    }     

    
    public static Claims decodeJWTToken(String token) throws Exception {
    	Map<String, String> configMap = getJWTParams();
    	
    	Claims claims = null;
    	try {
        	//claims = Jwts.parser().setSigningKey(configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY)).parseClaimsJws(token).getBody();    	
    	
			String secret = configMap.get(COD_NUM_INT_JWT_COD_PAR_AUTH_SECRET_KEY);
			byte[] keyBytes = Base64.getDecoder().decode(secret);
			Key key = Keys.hmacShaKeyFor(keyBytes);

			claims = Jwts.parser()
				.setSigningKey(key)
				.build()
				.parseClaimsJws(token)
				.getBody();			

    	} catch (ExpiredJwtException e) {
    		e.printStackTrace();
    		//throw new MsgExcept("Ocorreu um erro na tentativa de recuperar informações ao sistema. Contate o Administrador (JWT Expired Error).");
    	} catch (Exception e) {
    		e.printStackTrace();
    		//throw new MsgExcept("Ocorreu um erro na tentativa de recuperar informações ao sistema. Contate o Administrador (JWT Error).");
    	}
    	return claims;
	}
    
    public static String getDataFromDecodeJWTToken(String token) throws Exception {
    	Claims claims = decodeJWTToken(token);
    	String data   = "";
    	if (claims != null) {
    		data          = (String) claims.get("data");
    	}
    	return data;    	
    }
    
    public static String generateJWTTokenNovaPrev(Map<String, String> mapValores) throws Exception {
		StringBuilder sbJsonNovaPrev = new StringBuilder();
		sbJsonNovaPrev.append("{");
		sbJsonNovaPrev.append("\"").append("ambiente").             append("\"").append(":").append("\"").append(mapValores.get("ambiente")).             append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("codIns").               append("\"").append(":").append("\"").append(mapValores.get("codIns")).               append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("login").                append("\"").append(":").append("\"").append(mapValores.get("login")).                append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("numCpfLogin").          append("\"").append(":").append("\"").append(mapValores.get("numCpfLogin")).          append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("origem").               append("\"").append(":").append("\"").append(mapValores.get("origem")).               append("\"").append(",");
		sbJsonNovaPrev.append("}");
		return generateJWTToken(COD_NUM_INT_JWT_NOVAPREV, sbJsonNovaPrev.toString());
    }  
    
    public static String generateJWTToken(String codNum, Map<String, String> mapValores) throws Exception {
		StringBuilder sbJsonNovaPrev = new StringBuilder();
		sbJsonNovaPrev.append("{");
		sbJsonNovaPrev.append("\"").append("ambiente").             append("\"").append(":").append("\"").append(mapValores.get("ambiente")).             append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("codIns").               append("\"").append(":").append("\"").append(mapValores.get("codIns")).               append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("login").                append("\"").append(":").append("\"").append(mapValores.get("login")).                append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("numCpfLogin").          append("\"").append(":").append("\"").append(mapValores.get("numCpfLogin")).          append("\"").append(",");
		sbJsonNovaPrev.append("\"").append("origem").               append("\"").append(":").append("\"").append(mapValores.get("origem")).               append("\"").append(",");
		sbJsonNovaPrev.append("}");
		return generateJWTToken(codNum, sbJsonNovaPrev.toString());
    }  
    
}