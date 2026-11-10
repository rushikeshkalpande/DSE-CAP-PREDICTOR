package com.dsecap.predictor;

import java.io.*;
import java.util.Scanner;

/**
 * Simple test program to check which input method works in your environment
 */
public class InputTest {
    
    public static void main(String[] args) {
        System.out.println("=== Input Method Test ===");
        System.out.println("Testing different input methods to find what works...\n");
        
        // Test Method 1: System.console()
        testConsoleMethod();
        
        // Test Method 2: Scanner with System.in
        testScannerMethod();
        
        // Test Method 3: BufferedReader
        testBufferedReaderMethod();
        
        // Test Method 4: InputStreamReader
        testInputStreamReaderMethod();
    }
    
    private static void testConsoleMethod() {
        System.out.println("--- Test 1: System.console() ---");
        try {
            Console console = System.console();
            if (console != null) {
                System.out.print("Enter a test number using console: ");
                String input = console.readLine();
                System.out.println("✅ Console method works! You entered: " + input);
            } else {
                System.out.println("❌ Console is null (probably running in IDE)");
            }
        } catch (Exception e) {
            System.out.println("❌ Console method failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testScannerMethod() {
        System.out.println("--- Test 2: Scanner with System.in ---");
        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Enter a test number using Scanner: ");
            System.out.flush();
            
            if (scanner.hasNextLine()) {
                String input = scanner.nextLine();
                System.out.println("✅ Scanner method works! You entered: " + input);
            } else {
                System.out.println("❌ Scanner has no next line");
            }
        } catch (Exception e) {
            System.out.println("❌ Scanner method failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testBufferedReaderMethod() {
        System.out.println("--- Test 3: BufferedReader ---");
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            System.out.print("Enter a test number using BufferedReader: ");
            System.out.flush();
            
            String input = reader.readLine();
            System.out.println("✅ BufferedReader method works! You entered: " + input);
        } catch (Exception e) {
            System.out.println("❌ BufferedReader method failed: " + e.getMessage());
        }
        System.out.println();
    }
    
    private static void testInputStreamReaderMethod() {
        System.out.println("--- Test 4: InputStreamReader ---");
        try {
            InputStreamReader reader = new InputStreamReader(System.in);
            System.out.print("Enter a test number using InputStreamReader: ");
            System.out.flush();
            
            StringBuilder input = new StringBuilder();
            int ch;
            while ((ch = reader.read()) != -1 && ch != '\n' && ch != '\r') {
                input.append((char) ch);
            }
            
            System.out.println("✅ InputStreamReader method works! You entered: " + input.toString());
        } catch (Exception e) {
            System.out.println("❌ InputStreamReader method failed: " + e.getMessage());
        }
        System.out.println();
    }
}