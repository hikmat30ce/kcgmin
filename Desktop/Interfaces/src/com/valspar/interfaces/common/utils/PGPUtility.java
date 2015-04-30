package com.valspar.interfaces.common.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.security.Security;
import java.util.Date;
import java.util.Iterator;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.bcpg.ArmoredOutputStream;
import org.bouncycastle.bcpg.CompressionAlgorithmTags;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.openpgp.PGPCompressedData;
import org.bouncycastle.openpgp.PGPCompressedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedData;
import org.bouncycastle.openpgp.PGPEncryptedDataGenerator;
import org.bouncycastle.openpgp.PGPEncryptedDataList;
import org.bouncycastle.openpgp.PGPException;
import org.bouncycastle.openpgp.PGPLiteralData;
import org.bouncycastle.openpgp.PGPLiteralDataGenerator;
import org.bouncycastle.openpgp.PGPOnePassSignatureList;
import org.bouncycastle.openpgp.PGPPrivateKey;
import org.bouncycastle.openpgp.PGPPublicKey;
import org.bouncycastle.openpgp.PGPPublicKeyEncryptedData;
import org.bouncycastle.openpgp.PGPPublicKeyRing;
import org.bouncycastle.openpgp.PGPPublicKeyRingCollection;
import org.bouncycastle.openpgp.PGPSecretKey;
import org.bouncycastle.openpgp.PGPSecretKeyRingCollection;
import org.bouncycastle.openpgp.PGPUtil;
import org.bouncycastle.openpgp.jcajce.JcaPGPObjectFactory;
import org.bouncycastle.openpgp.operator.jcajce.JcaKeyFingerprintCalculator;
import org.bouncycastle.openpgp.operator.jcajce.JcePBESecretKeyDecryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePGPDataEncryptorBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyDataDecryptorFactoryBuilder;
import org.bouncycastle.openpgp.operator.jcajce.JcePublicKeyKeyEncryptionMethodGenerator;
import org.bouncycastle.util.io.Streams;

public class PGPUtility
{
  private static Logger log4jLogger = Logger.getLogger(PGPUtility.class);

  static
  {
    Security.addProvider(new BouncyCastleProvider());
  }
  
  public PGPUtility()
  {
  }
/*
  public static void main(String[] args)
  {
    try
    {
      System.out.println("Starting PGPUtility...");
      PGPUtility pGPUtility = new PGPUtility();
      pGPUtility.encryptFile("C:\\Users\\djemin\\Documents\\PortablePGP\\pgpUtilityEncryptedFile.txt", "C:\\Users\\djemin\\Documents\\PortablePGP\\test.txt", "C:\\Users\\djemin\\Documents\\PortablePGP\\PortablePGPPublicKey.pub", false, true);
      pGPUtility.decryptFile("C:\\Users\\djemin\\Documents\\PortablePGP\\pgpUtilityEncryptedFile.txt", "C:\\Users\\djemin\\Documents\\PortablePGP\\PortablePGPPrivateKey.ppk", "valspar1", "C:\\Users\\djemin\\Documents\\PortablePGP\\it_worked.txt");
      System.out.println("PGPUtility Complete");
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
  }
*/
  private static byte[] compressFile(String fileName, int algorithm)
  {
    ByteArrayOutputStream byteArrayOutputStream = null;

    try
    {
      byteArrayOutputStream = new ByteArrayOutputStream();
      PGPCompressedDataGenerator comData = new PGPCompressedDataGenerator(algorithm);
      writeFile(comData.open(byteArrayOutputStream), PGPLiteralData.BINARY, new File(fileName));
      comData.close();
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    return byteArrayOutputStream.toByteArray();
  }

  private static void writeFile(OutputStream out, char fileType, File file)
  {
    OutputStream os = null;
    FileInputStream fis = null;
    
    try
    {
      PGPLiteralDataGenerator pGPLiteralDataGenerator = new PGPLiteralDataGenerator();
      os = pGPLiteralDataGenerator.open(out, fileType, "", file.length(), new Date(file.lastModified()));
      fis = new FileInputStream(file);
      IOUtils.write(IOUtils.toByteArray(fis), os);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      IOUtils.closeQuietly(os);
      IOUtils.closeQuietly(fis);
    }
  }

  private static PGPPrivateKey findSecretKey(PGPSecretKeyRingCollection pGPSecretKeyRingCollection, long keyID, String password)
  {
    PGPPrivateKey pGPPrivateKey = null;
    try
    {
      PGPSecretKey pGPSecretKey = pGPSecretKeyRingCollection.getSecretKey(keyID);
  
      if (pGPSecretKey != null)
      {
        pGPPrivateKey = pGPSecretKey.extractPrivateKey(new JcePBESecretKeyDecryptorBuilder().setProvider("BC").build(password.toCharArray()));
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }

    return pGPPrivateKey;
  }

  private static PGPPublicKey readPublicKey(String fileName)
  {
    InputStream is = null;
    PGPPublicKey pGPPublicEncryptionKey = null;

    try
    {
      is = new BufferedInputStream(new FileInputStream(fileName));
  
      PGPPublicKeyRingCollection pGPPublicKeyRingCollection = new PGPPublicKeyRingCollection(PGPUtil.getDecoderStream(is), new JcaKeyFingerprintCalculator());
  
      Iterator keyRingIterator = pGPPublicKeyRingCollection.getKeyRings();
      
      while (keyRingIterator.hasNext())
      {
        PGPPublicKeyRing pGPPublicKeyRing = (PGPPublicKeyRing) keyRingIterator.next();
        Iterator keyIterator = pGPPublicKeyRing.getPublicKeys();
        while (keyIterator.hasNext())
        {
          PGPPublicKey pGPPublicKey = (PGPPublicKey) keyIterator.next();
  
          if (pGPPublicKey.isEncryptionKey())
          {
            pGPPublicEncryptionKey = pGPPublicKey;
          }
        }
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      IOUtils.closeQuietly(is);    
    }
    return pGPPublicEncryptionKey;
  }

  public static void decryptFile(String inputFileName, String keyFileName, String password, String outputFileName)
  {
    InputStream fileInputStream = null;
    InputStream keyInputStream = null;

    try
    {
      fileInputStream = PGPUtil.getDecoderStream(new BufferedInputStream(new FileInputStream(inputFileName)));
      keyInputStream = new BufferedInputStream(new FileInputStream(keyFileName));

      JcaPGPObjectFactory jcaPGPObjectFactory = new JcaPGPObjectFactory(fileInputStream);
      PGPEncryptedDataList pGPEncryptedDataList;
      Object obj = jcaPGPObjectFactory.nextObject();

      if (obj instanceof PGPEncryptedDataList)
      {
        pGPEncryptedDataList = (PGPEncryptedDataList) obj;
      }
      else
      {
        pGPEncryptedDataList = (PGPEncryptedDataList) jcaPGPObjectFactory.nextObject();
      }

      PGPPrivateKey pGPPrivateKey = null;
      PGPPublicKeyEncryptedData pGPPublicKeyEncryptedData = null;
      PGPSecretKeyRingCollection pGPSecretKeyRingCollection = new PGPSecretKeyRingCollection(PGPUtil.getDecoderStream(keyInputStream), new JcaKeyFingerprintCalculator());
      Iterator i = pGPEncryptedDataList.getEncryptedDataObjects();

      while (pGPPrivateKey == null && i.hasNext())
      {
        pGPPublicKeyEncryptedData = (PGPPublicKeyEncryptedData) i.next();
        pGPPrivateKey = PGPUtility.findSecretKey(pGPSecretKeyRingCollection, pGPPublicKeyEncryptedData.getKeyID(), password);
      }

      if (pGPPrivateKey == null)
      {
        throw new IllegalArgumentException("secret key for message not found.");
      }

      InputStream privateKeyInputStream = pGPPublicKeyEncryptedData.getDataStream(new JcePublicKeyDataDecryptorFactoryBuilder().setProvider("BC").build(pGPPrivateKey));
      JcaPGPObjectFactory privateKeyJcaPGPObjectFactory = new JcaPGPObjectFactory(privateKeyInputStream);
      Object message = privateKeyJcaPGPObjectFactory.nextObject();

      if (message instanceof PGPCompressedData)
      {
        PGPCompressedData pGPCompressedData = (PGPCompressedData) message;
        JcaPGPObjectFactory compressedDataJcaPGPObjectFactory = new JcaPGPObjectFactory(pGPCompressedData.getDataStream());
        message = compressedDataJcaPGPObjectFactory.nextObject();
      }

      if (message instanceof PGPLiteralData)
      {
        PGPLiteralData pGPLiteralData = (PGPLiteralData) message;

        String literalDataFileName = pGPLiteralData.getFileName();
        if (StringUtils.isEmpty(literalDataFileName))
        {
          literalDataFileName = outputFileName;
        }

        InputStream is = pGPLiteralData.getInputStream();
        OutputStream os = new BufferedOutputStream(new FileOutputStream(literalDataFileName));
        Streams.pipeAll(is, os);
        os.close();
        is.close();
      }
      else if (message instanceof PGPOnePassSignatureList)
      {
        throw new PGPException("encrypted message contains a signed message - not literal data.");
      }
      else
      {
        throw new PGPException("message is not a simple encrypted file - type unknown.");
      }

      if (pGPPublicKeyEncryptedData.isIntegrityProtected())
      {
        if (!pGPPublicKeyEncryptedData.verify())
        {
          log4jLogger.error("message failed integrity check");
        }
      }
      else
      {
        log4jLogger.error("no message integrity check");
      }
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      IOUtils.closeQuietly(keyInputStream);
      IOUtils.closeQuietly(fileInputStream);
    }    
  }

  public static void encryptFile(String outputFileName, String inputFileName, String encKeyFileName, boolean armor, boolean withIntegrityCheck)
  {
    OutputStream fileOutputStream = null;
    OutputStream encryptedFileOutputStream = null;
    try
    {
      fileOutputStream = new BufferedOutputStream(new FileOutputStream(outputFileName));
      PGPPublicKey encKey = PGPUtility.readPublicKey(encKeyFileName);
  
      if (armor)
      {
        fileOutputStream = new ArmoredOutputStream(fileOutputStream);
      }

      byte[] bytes = PGPUtility.compressFile(inputFileName, CompressionAlgorithmTags.ZIP);
      PGPEncryptedDataGenerator pGPEncryptedDataGenerator = new PGPEncryptedDataGenerator(new JcePGPDataEncryptorBuilder(PGPEncryptedData.CAST5).setWithIntegrityPacket(withIntegrityCheck).setSecureRandom(new SecureRandom()).setProvider("BC"));
      pGPEncryptedDataGenerator.addMethod(new JcePublicKeyKeyEncryptionMethodGenerator(encKey).setProvider("BC"));
      encryptedFileOutputStream = pGPEncryptedDataGenerator.open(fileOutputStream, bytes.length);
      encryptedFileOutputStream.write(bytes);
    }
    catch (Exception e)
    {
      log4jLogger.error(e);
    }
    finally
    {
      IOUtils.closeQuietly(encryptedFileOutputStream);
      IOUtils.closeQuietly(fileOutputStream);
    }
  }
}
