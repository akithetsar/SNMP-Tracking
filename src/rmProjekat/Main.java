package rmProjekat;

import java.io.IOException;

public class Main {


	
	
	public static void main(String[] args) {
		
		try {
			Router[] routers = new Router[Config.numOfRouters];
			int i = 0;
			for(String ip : Config.ips) {
				routers[i] = new Router(ip);
				++i;
			}
			
			Monitor monitor = new Variant1(routers);
			monitor.start();
			
			
		} catch (IOException e) {
			// TODO Auto-generated catch block
//			e.printStackTrace();
		}
		
		
	}
};