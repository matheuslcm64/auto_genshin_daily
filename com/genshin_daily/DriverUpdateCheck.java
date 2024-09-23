package com.genshin_daily;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
//import org.openqa.selenium.remote.CapabilityType;

public class DriverUpdateCheck {
	
	private String build_version_regex(String version) {
        String[] browser_version_numbers = version.split("\\.");
		String regex="";
		for(int i=0; i<browser_version_numbers.length-1;i++) {
			regex+=browser_version_numbers[i]+"\\.";
		}
		regex+=browser_version_numbers[browser_version_numbers.length-1];
		return regex;
	}
	
	private String build_regex(String[] browser_version_numbers,int numbers_to_match) {
		if(browser_version_numbers.length < numbers_to_match || numbers_to_match<=0)
			return null;
		if(browser_version_numbers.length == numbers_to_match) {
			String version = "";
			for(String s: browser_version_numbers)
				version+=s;
			return version;
		}
				
		String regex="";
		for(int i=0; i<numbers_to_match;i++) {
			regex+=browser_version_numbers[i]+"\\.";
		}
		for(int i=0; i<(browser_version_numbers.length - numbers_to_match)-1;i++) {
			regex+="\\d+\\.";
		}

		regex+="\\d+";
		return regex;
	}
	
	
	
	private String get_json(String url, HttpClient client) {
		try {
			// Create HttpRequest object
			HttpRequest request = HttpRequest.newBuilder()
			    .uri(new URI(url))
			    .GET()
			    .build();
			// Send the request and get the response as a string
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
			
			if (response.statusCode() == 200) {
				String json = response.body();
				return json;
			} else {
				System.out.println("GET request on "+url+" failed. Response Code: " + response.statusCode());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
		
		return null;
	}
	
	private String get_latest_driver_version(String json,String browser_version,int... numbers_to_match1) {
        int numbers_to_match = numbers_to_match1.length>0 ? numbers_to_match1[0] : 3;
		String[] browser_version_numbers = browser_version.split("\\.");
		int[] browser_version_numbers_int = Arrays.stream(browser_version_numbers).mapToInt(Integer::parseInt).toArray();
		String most_updated_version = null;
		int[] most_updated_version_numbers = new int[browser_version_numbers.length];
		Arrays.fill(most_updated_version_numbers, -1);
		List<String> visited_versions = new ArrayList<>();
        while(numbers_to_match>0) {
        	String regex = build_regex(browser_version_numbers, numbers_to_match);
        	if(regex!=null) {
        		Pattern pattern = Pattern.compile(regex);
        		Matcher matcher  = pattern.matcher(json);
        		while(matcher.find()) {
        			
        			String version = matcher.group();
        			if(!visited_versions.contains(version)) {
        				visited_versions.add(version);
        				int[] version_numbers = Arrays.stream(version.split("\\."))
        						.mapToInt(Integer::parseInt)
        						.toArray();
        				boolean first_it=true;
        				for (int i=numbers_to_match; i<browser_version_numbers_int.length; i++) {
        					if(first_it) {
        						if(version_numbers[i]>browser_version_numbers_int[i]) {
        							break;
        						}
        						if(version_numbers[i]>most_updated_version_numbers[i]) {
        							most_updated_version_numbers[i] = version_numbers[i];
        							most_updated_version = version;
        							//Found the highest valid majora? Reset the following positions
    								for(int j=i+1; j<browser_version_numbers_int.length; j++) {
    									most_updated_version_numbers[j] = -1;
    								}        								
        						}        						
        					}else {
        						if(version_numbers[i]>most_updated_version_numbers[i]) {
        							most_updated_version_numbers[i] = version_numbers[i];
        							most_updated_version = version;
        						}          						
        					}
        					first_it = false;
        				}
        			}
        			
        		}
        		if(most_updated_version!=null) {
        			return most_updated_version;
        		}
        	}
        	numbers_to_match--;
        }
        
        return most_updated_version;
	}

	
	private String get_latest_driver_url(String json, String version_regex) {
		
        String regex1 = "(?<=\"version\":\""+version_regex+"\",)(.*?chromedriver-win64\\.zip)";
        String regex2 = "(?<=\"chromedriver\":)(.*)";
        String regex3 = "(?<=\"platform\":\"win64\",\"url\":\")(.*)";
        Pattern pattern = Pattern.compile(regex1);
        String answer;

        // Create a matcher to find the pattern in the JSON data
        Matcher matcher = pattern.matcher(json);

        // Check if the pattern is found
        if (matcher.find()) {
        	
        	answer = matcher.group();
        	pattern = Pattern.compile(regex2);
        	matcher = pattern.matcher(answer);
       		answer = matcher.find() ? matcher.group() : null;
       		if(answer!=null) {
       			pattern = Pattern.compile(regex3);
       			matcher = pattern.matcher(answer);
       			answer = matcher.find() ? matcher.group() : null; 
       			if(answer!=null) {
       				return answer;
       			}
       		}
        } else {
            // If no match is found
            System.out.println("No match found.");
        }
		
		return null;
	}
	
    private void downloadZipFile(String fileUrl, String saveFilePath){
        try {
        	
        	// Create URL object from the download link
        	URI uri = new URI(fileUrl);
        	URL url = uri.toURL();
        	HttpURLConnection httpConn = (HttpURLConnection) url.openConnection();
        	int responseCode = httpConn.getResponseCode();
        	
        	// Check if the request was successful
        	if (responseCode == HttpURLConnection.HTTP_OK) {
        		// Open input stream from the connection
        		InputStream inputStream = httpConn.getInputStream();
        		// Create output stream to save the file
        		FileOutputStream outputStream = new FileOutputStream(saveFilePath);
        		
        		// Buffer to hold data
        		byte[] buffer = new byte[4096];
        		int bytesRead = -1;
        		
        		// Read from input stream and write to output stream
        		while ((bytesRead = inputStream.read(buffer)) != -1) {
        			outputStream.write(buffer, 0, bytesRead);
        		}
        		
        		// Close the streams
        		outputStream.close();
        		inputStream.close();
        		
        		System.out.println("File downloaded to " + saveFilePath);
        	} else {
        		System.out.println("No file to download. Server returned HTTP code: " + responseCode);
        	}
        	
        	httpConn.disconnect();
        	
        }catch(Exception e) {
        	e.printStackTrace();
        }
    }
    
    private void unzip(String zipFilePath, String destDirectory) throws IOException {
        // Create destination directory if it doesn't exist
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdir();
        }

        // Open the zip file stream
        FileInputStream fis = new FileInputStream(zipFilePath);
        ZipInputStream zis = new ZipInputStream(fis);
        ZipEntry entry;

        // Process each entry in the zip file
        while ((entry = zis.getNextEntry()) != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            filePath = filePath.replace("/", "\\");
            if (entry.isDirectory()) {
                // If the entry is a directory, create it
                File dir = new File(filePath);
                dir.mkdirs();
            }else {
            	File file = new File(filePath);
            	File parentDir = file.getParentFile();
            	if(!parentDir.exists()) {
            		parentDir.mkdirs();
            	}
            	//Extract file
        		extractFile(zis, filePath);
        		System.out.println("Extracted: "+filePath);
            }
            zis.closeEntry();
        }

        zis.close();
        fis.close();
    }

    // Extract file method
    private void extractFile(ZipInputStream zis, String filePath) throws IOException {
        // Create output file
        FileOutputStream fos = new FileOutputStream(filePath);
        byte[] buffer = new byte[4096];
        int bytesRead;

        // Read the file and write to the output
        while ((bytesRead = zis.read(buffer)) != -1) {
            fos.write(buffer, 0, bytesRead);
        }

        fos.close();
    }
    
    public void deletePath(Path folderToDelete) {
        // Define the folder to delete
        //Path folderToDelete = Paths.get("C:\\Drivers\\chromedriver-win64");

        try {
            // Recursively delete the folder and its contents
            Files.walk(folderToDelete)
                .sorted(Comparator.reverseOrder()) // Sort to delete files before directories
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Deleted: " + path);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                });

            System.out.println("Folder and all its contents deleted successfully.");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private void searchMoveFile(String dest, String file) {
        try {
            // Define the root parent and file name
            Path rootParent = Paths.get(dest);
            String fileName = file;

            // Search for the file in the directory tree
            Optional<Path> sourcePath = Files.walk(rootParent)
                    .filter(path -> path.getFileName().toString().equals(fileName))
                    .findFirst();

            // If the file is found, move it to the rootParent directory
            if (sourcePath.isPresent()) {
                Path targetPath = rootParent.resolve(fileName);

                // Move the file
                Files.move(sourcePath.get(), targetPath, StandardCopyOption.REPLACE_EXISTING);

                System.out.println("File moved successfully from " + sourcePath.get() + " to " + targetPath);
            } else {
                System.out.println("File not found.");
            }
            deletePath(sourcePath.get().getParent());
        
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
	

	/*
		Check the chromedriver compatibility with the browser and in 
    	case of failure download the appropriate driver version
    */
	public boolean check() {
		
      try {
          // Replace with the path to your WebDriver executable
          String driverPath = "C:\\Drivers\\chromedriver-win64-118.0.5964.0.exe"; // For ChromeDriver

/*//Chrome version through registry
          // Command to read Chrome version from Windows registry
          String[] command = {"reg", "query", "HKEY_CURRENT_USER\\Software\\Google\\Chrome\\BLBeacon", "/v", "version"};
          
          // Execute the command
          process = Runtime.getRuntime().exec(command);
          
          // Read the output of the command
          reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
          String chrome_version = null;
          while ((line = reader.readLine()) != null) {
              if (line.contains("version")) {
                  // Extract the version number from the output line
            	  String[] s;
            	  s = line.split("\\s+");
            	  chrome_version = s[s.length - 1];
                  break;
              }
          }
          
          // Wait for the process to complete
          process.waitFor();
          
          // Print the Chrome version
          if (chrome_version != null) {
              System.out.println("Chrome version: " + chrome_version);
          } else {
              System.out.println("Chrome version not found.");
          }
*/          

//Chrome version through selenium          
          System.setProperty("webdriver.chrome.driver", driverPath);          
          // Initialize the ChromeDriver
          WebDriver driver = new ChromeDriver();

          // Retrieve the browser version using capabilities
          //String chromeVersion = ((RemoteWebDriver) driver).getCapabilities().getVersion();

          // Print the Chrome browser version
          //System.out.println("Chrome version: " + chromeVersion);
          
          
          // Close the browser
          driver.quit();
          System.out.println("All fine");
          return true;
 
      } catch (org.openqa.selenium.SessionNotCreatedException e) {
    	  e.printStackTrace();
    	  String regex = "(?<=Current browser version is )\\d+\\.\\d+\\.\\d+\\.\\d+(?=\\s)";
    	  Matcher match = Pattern.compile(regex).matcher(e.getMessage());
    	  if(match.find()) {
    		  try{
        		  String chrome_version = match.group();  
        		  System.out.println(chrome_version);
        		  
                  // Replace with the path to your WebDriver executable
        		  String driverPath = "C:\\Drivers";
                  String driverSavePath = driverPath+"\\chromedriver.zip"; // For ChromeDriver
                  
                  // Create HttpClient object
                  HttpClient client = HttpClient.newHttpClient();
                  String apiVersionsURL = "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions.json";
                  String apiDownloadURL = "https://googlechromelabs.github.io/chrome-for-testing/known-good-versions-with-downloads.json";
                  String version_json;
                  String download_json;
                  
                  version_json = get_json(apiVersionsURL, client);
                  if (version_json!=null) {
                	  String latest_driver_version = get_latest_driver_version(version_json, chrome_version);
                	  download_json = get_json(apiDownloadURL, client);
                	  String latest_version_regex = build_version_regex(latest_driver_version);
                	  String latest_driver_url = get_latest_driver_url(download_json, latest_version_regex);
                	  System.out.println(latest_driver_url);
                	  downloadZipFile(latest_driver_url, driverSavePath);
                	  unzip(driverSavePath, driverPath);
                	  searchMoveFile(driverPath, "chromedriver.exe");
                	  return true;
                  }
    			  
    		  } catch (Exception e2) {
    	          e2.printStackTrace();
    	      }

    	  }else {
              System.out.println("No match found.");
    	  }
      } catch (Exception e) {
          e.printStackTrace();
      }
      return false;
	}
}
