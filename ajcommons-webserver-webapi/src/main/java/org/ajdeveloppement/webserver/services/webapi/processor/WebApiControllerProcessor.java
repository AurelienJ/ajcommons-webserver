/*
 * Créé le 12 juin 2012 à 21:57:18 pour AjCommons (Bibliothèque de composant communs)
 *
 * Copyright 2002-2012 - Aurélien JEOFFRAY
 *
 * http://www.ajdeveloppement.org
 *
 * *** CeCILL-C Terms *** 
 *
 * FRANCAIS:
 *
 * Ce logiciel est régi par la licence CeCILL-C soumise au droit français et
 * respectant les principes de diffusion des logiciels libres. Vous pouvez
 * utiliser, modifier et/ou redistribuer ce programme sous les conditions
 * de la licence CeCILL-C telle que diffusée par le CEA, le CNRS et l'INRIA 
 * sur le site "http://www.cecill.info".
 *
 * En contrepartie de l'accessibilité au code source et des droits de copie,
 * de modification et de redistribution accordés par cette licence, il n'est
 * offert aux utilisateurs qu'une garantie limitée.  Pour les mêmes raisons,
 * seule une responsabilité restreinte pèse sur l'auteur du programme,  le
 * titulaire des droits patrimoniaux et les concédants successifs.
 *
 * A cet égard  l'attention de l'utilisateur est attirée sur les risques
 * associés au chargement,  à l'utilisation,  à la modification et/ou au
 * développement et à la reproduction du logiciel par l'utilisateur étant 
 * donné sa spécificité de logiciel libre, qui peut le rendre complexe à 
 * manipuler et qui le réserve donc à des développeurs et des professionnels
 * avertis possédant  des  connaissances  informatiques approfondies.  Les
 * utilisateurs sont donc invités à charger  et  tester  l'adéquation  du
 * logiciel à leurs besoins dans des conditions permettant d'assurer la
 * sécurité de leurs systèmes et ou de leurs données et, plus généralement, 
 * à l'utiliser et l'exploiter dans les mêmes conditions de sécurité. 
 * 
 * Le fait que vous puissiez accéder à cet en-tête signifie que vous avez 
 * pri connaissance de la licence CeCILL-C, et que vous en avez accepté les
 * termes.
 *
 * ENGLISH:
 * 
 * This software is governed by the CeCILL-C license under French law and
 * abiding by the rules of distribution of free software.  You can  use, 
 * modify and/ or redistribute the software under the terms of the CeCILL-C
 * license as circulated by CEA, CNRS and INRIA at the following URL
 * "http://www.cecill.info". 
 *
 * As a counterpart to the access to the source code and  rights to copy,
 * modify and redistribute granted by the license, users are provided only
 * with a limited warranty  and the software's author,  the holder of the
 * economic rights,  and the successive licensors  have only  limited
 * liability. 
 * 
 * In this respect, the user's attention is drawn to the risks associated
 * with loading,  using,  modifying and/or developing or reproducing the
 * software by the user in light of its specific status of free software,
 * that may mean  that it is complicated to manipulate,  and  that  also
 * therefore means  that it is reserved for developers  and  experienced
 * professionals having in-depth computer knowledge. Users are therefore
 * encouraged to load and test the software's suitability as regards their
 * requirements in conditions enabling the security of their systems and/or 
 * data to be ensured and,  more generally, to use and operate it in the 
 * same conditions as regards security. 
 * 
 * The fact that you are presently reading this means that you have had
 * knowledge of the CeCILL-C license and that you accept its terms.
 */
package org.ajdeveloppement.webserver.services.webapi.processor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Set;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic.Kind;
import javax.tools.FileObject;
import javax.tools.StandardLocation;

import org.ajdeveloppement.webserver.services.webapi.annotations.WebApiController;

/**
 * @author aurelien
 *
 */
@SupportedAnnotationTypes(value={"org.ajdeveloppement.webserver.services.webapi.annotations.WebApiController"}) //
@SupportedSourceVersion(SourceVersion.RELEASE_8)
public class WebApiControllerProcessor extends AbstractProcessor {

	private boolean controllersFileGenerated = false;
	private String controllers = ""; //$NON-NLS-1$
	
	@Override
	public synchronized void init(ProcessingEnvironment processingEnv) {
		super.init(processingEnv);
	}
	

	/* (non-Javadoc)
	 * @see javax.annotation.processing.AbstractProcessor#process(java.util.Set, javax.annotation.processing.RoundEnvironment)
	 */
	@Override
	@SuppressWarnings({ "nls" })
	public boolean process(Set<? extends TypeElement> elements, RoundEnvironment env) {
		
		if(!env.processingOver()) {
			if(elements == null)
				return false;
			
			if(!controllersFileGenerated) {
				try {
					
					if(controllers.isEmpty()) {
						try {
							FileObject fo = processingEnv.getFiler().getResource(StandardLocation.CLASS_OUTPUT, 
									"", "META-INF/controllers");
							if(fo != null) {
								try(BufferedReader reader = new BufferedReader(fo.openReader(true))) {
									String line = null;
									while((line = reader.readLine()) != null) {
										controllers += line + "\n";
									}
								}
							}
						} catch (IOException e) {
							//Normal case
						}
					}
	
					for (Element element : env.getElementsAnnotatedWith(WebApiController.class)) {
						String controllerName = ((TypeElement)element).getQualifiedName().toString();
						if(!controllers.contains(controllerName))
							controllers += controllerName + "\n";
					}
	
					FileObject fo = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, 
								"", "META-INF/controllers");
					
					fo.openWriter().append(controllers).close();
					
					controllersFileGenerated = true;
				} catch (IOException e) {
					e.printStackTrace();
					processingEnv.getMessager().printMessage(Kind.ERROR, getStackTrace(e));
				}
			}
		}
		return false;
	}
	

	private static String getStackTrace(Throwable aThrowable) {
		final Writer result = new StringWriter();
		final PrintWriter printWriter = new PrintWriter(result);
		aThrowable.printStackTrace(printWriter);
		return result.toString();
	}
}
