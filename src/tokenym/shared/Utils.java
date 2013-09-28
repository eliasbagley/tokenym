package tokenym.shared;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.*;
import java.util.Random;
import java.util.Scanner;

/**
 * <p>
 * FieldVerifier validates that the name the user enters is valid.
 * </p>
 * <p>
 * This class is in the <code>shared</code> package because we use it in both the client code and on the server. On the
 * client, we verify that the name is valid before sending an RPC request so the user doesn't have to wait for a network
 * round trip to get feedback. On the server, we verify that the name is correct to ensure that the input is correct
 * regardless of where the RPC originates.
 * </p>
 * <p>
 * When creating a class that is used on both the client and the server, be sure that all code is translatable and does
 * not use native JavaScript. Code that is not translatable (such as code that interacts with a database or the file
 * system) cannot be compiled into client side JavaScript. Code that uses native JavaScript (such as Widgets) cannot be
 * run on the server.
 * </p>
 */
public class Utils
{

    /**
     * Verifies that the specified name is valid for our service.
     * 
     * In this example, we only require that the name is at least four characters. In your application, you can use more
     * complex checks to ensure that usernames, passwords, email addresses, URLs, and other fields have the proper
     * syntax.
     * 
     * @param name
     *            the name to validate
     * @return true if valid, false if invalid
     */
    // TODO make this a more robust password checker

    private static int    numRows        = 6;
    private static int    numCols        = 6;
    private static int    numTokenDigits = 10;
    private static char[] charArray      = new char[] { 'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z', '0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9'               };

    public static boolean isValidPassword (String password, Boolean basic)
    {
        /*
         *  ^                 # start-of-string
            (?=.*[0-9])       # a digit must occur at least once
(?=.*[a-z])       # a lower case letter must occur at least once
(?=.*[A-Z])       # an upper case letter must occur at least once
(?=.*[@#$%^&+=])  # a special character must occur at least once
(?=\S+$)          # no whitespace allowed in the entire string
.{8,}             # anything, at least eight places though
$                 # end-of-string
         */
        // either the password has to match the strict rules, or it has to be super long (20 chars)
        if (basic && !(password.matches("^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[!@#$%^&*+=-_()])(?=\\S+$).{8,}$") || password.length() > 10))
        {
            return false;
        }
        else if(!basic && password.length() < 6)
        { 
           return false;
        }
        
        return true;
    }

    public static String escapeHtml (String html)
    {
        if (html == null)
        {
            return null;
        }
        return html.replaceAll("&", "&amp;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
    }

    
    // make this prettier
    public static String formatKeyboard(String keyboard)
    {
        //String formattedKeyboard = keyboard.replace("\n", "<br>");

        // split it by <br> tags
        String[] splitKeyboard = keyboard.split("\n");

        // this block of code just puts spaces inbetween each character in the keyboard
        String newKeyboard = "";
        for (int i = 0; i < splitKeyboard.length; i++)
        {
            String temp = "";
            String temp2 = splitKeyboard[i];
            for (int j = 0; j < temp2.length(); j++)
            {
                temp = temp + temp2.charAt(j) + " ";
            }
            newKeyboard = newKeyboard + temp + "\n";
        }

      return newKeyboard;
    }
    
    public static String createUniqueToken ()
    {
        Random r = new Random(System.currentTimeMillis());
        String s = "";

        for (int i = 0; i < numTokenDigits; i++)
        {
            s += r.nextInt(10); // can also use the randomized char array right here, if we want tokens to contain
                                // alphanumeric
        }

        return s;
    }

    // 1 == row column grid
    // 2 == full match grid
    public static String createGrid (int gridOption)
    {
        String grid = ""; // the "grid" is 4x10, but it will be 14 characters long: the first 4 being the 4, the last 10
                          // being the 10

        if (gridOption == 1)
        {
            shuffle(charArray);
            int index = 0;

            for (int i = 0; i < numRows; i++)
            {
                // grid += (char)((int)'!' + r.nextInt(90)); // these need to not have duplicates
                grid += charArray[index++];
            }
            grid += "\n";
            for (int i = 0; i < numCols; i++)
            {
                // grid += (char)((int)'!' + r.nextInt(90)); // these need to not have duplicates
                grid += charArray[index++];
            }
        }
        else if (gridOption == 2)
        {
            grid = createKeyboard(); // a keyboard and grid2 are the same            
        }

        return grid;
    }

    // a non-biased shuffle
    public static void shuffle (char[] c)
    {
        Random r = new Random(System.currentTimeMillis());
        for (int i = c.length - 1; i >= 0; i--)
        {
            int index = r.nextInt(i + 1);
            char temp = c[index];
            c[index] = c[i];
            c[i] = temp;
        }

    }

    public static String createKeyboard () // this can be used to create a grid too
    {

        String keyboard = "";
        // add more characters to this array later
        // move this array to a static array

        shuffle(charArray);

        int index = 0;

        for (int i = 0; i < numRows; i++)
        {
            for (int j = 0; j < numCols; j++)
            {
                keyboard += charArray[index];
                index = (index + 1) % charArray.length;
            }
            keyboard += "\n";
        }

        return keyboard;
    }

    // hashing driver method
    public static String hash (String s) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        return hash(s, 3);
    }

    // is there any way I can protect this file so that there is no way it can be served?
    public static String hash (String s, int depth) throws NoSuchAlgorithmException, UnsupportedEncodingException
    {
        if (depth <= 0)
            return s + "$d0ne$"; // just more fun with salt;
        s = "Xx@XSuPeRSeCrEtSalT123XxX!" + hash(s, depth - 1) + hash(s, depth - 2) + "!ZzZSuP@eRSeCrEtSalT456ZzZ";
        MessageDigest m = MessageDigest.getInstance("SHA-512");
        byte[] bytes = m.digest(s.getBytes("UTF-8"));
        BigInteger bigInt = new BigInteger(1, bytes);

        return bigInt.toString(16);
    }

    // note: decrypt using rows then columns
    // needs to be tested
    public static String decrypt (String encryptedTokenym, String grid, String keyboard, int gridOption)
    {
        String tokenym = "";
        if (gridOption == 1) // row column lookup
        {
            // String gridRows = grid.substring(0, numRows);
            // String gridCols = grid.substring(numRows, grid.length());
            System.out.println("Grid: " + grid);
            String[] gridArray = grid.split("\n");
            String gridRows = gridArray[0];
            String gridCols = gridArray[1];

            char[][] kb = new char[numRows][numCols]; // needs to be tested

            // create the keyboard out of the string
            String[] kbRows = keyboard.split("\n");
            for (int i = 0; i < kbRows.length; i++)
            {
                // will need to do it the old way right here if I start adding spaces (split by space)
                for (int j = 0; j < kbRows[i].length(); j++)
                {
                    kb[i][j] = kbRows[i].charAt(j);
                }
            }

            for (int i = 0; i < encryptedTokenym.length(); i += 2)
            {
                char row = encryptedTokenym.charAt(i);
                char col = encryptedTokenym.charAt(i + 1);

                int r = gridRows.indexOf(row);
                int c = gridCols.indexOf(col);

                if (r == -1 || c == -1)
                {
                    // then this grid can't be used
                    return null;
                }

                tokenym += kb[r][c]; // make sure later on that these are in the right order
            }
        }
        else if (gridOption == 2)
        {
            // strip all newlines and spaces so the correspondence can be done correctly
            grid = grid.replaceAll("\n", "");
            keyboard = keyboard.replaceAll(" ", "");
            
            for (int i = 0; i < encryptedTokenym.length(); i++)
            {
                // find the characters position on the grid
                int pos = grid.indexOf(encryptedTokenym.charAt(i));
                tokenym += keyboard.charAt(pos);
            }
        }

        System.out.println("decrypted to: " + tokenym);
        return tokenym;
    }

}
