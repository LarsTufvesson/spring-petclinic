/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.samples.petclinic.owner;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.ModelAndView;

import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Metrics;

import javax.validation.Valid;
import java.util.Collection;
import java.util.Map;

/**
 * @author Juergen Hoeller
 * @author Ken Krebs
 * @author Arjen Poutsma
 * @author Michael Isvy
 */
@Controller
class OwnerController {
    private static final String LAT_REQUESTS_TOTAL = "lat_requests_total";
    private static final String LAT_TIME_PROCESS_FIND_FORM = "lat_time_processFindForm";
    private static final String VIEWS_OWNER_CREATE_OR_UPDATE_FORM = "owners/createOrUpdateOwnerForm";
    private static final String ID = "id";
    private static final String OWNERS_NEW = "/owners/new";
    private static final String OWNERS_FIND = "/owners/find";
    private static final String OWNER = "owner";
    private static final String OWNER_ID = "ownerId";
    private static final String SLASH_OWNERS = "/owners";
    private static final String REDIRECT_OWNERS = "redirect:/owners/";
    private static final String OWNERS_FIND_OWNERS = "owners/findOwners";
    private static final String LAST_NAME = "lastName";
    private static final String NOT_FOUND = "notFound";
    private static final String NOT_SPACE_FOUND = "not found";
    private static final String SELECTIONS = "selections";
    private static final String OWNERS_OWNERS_LIST = "owners/ownersList";
    private static final String OWNERS_OWNER_ID_EDIT = "/owners/{ownerId}/edit";

    private final OwnerRepository owners;
    private Counter requests = Metrics.counter(LAT_REQUESTS_TOTAL);


    public OwnerController(OwnerRepository clinicService) {
        this.owners = clinicService;
    }

    @InitBinder
    public void setAllowedFields(WebDataBinder dataBinder) {
        dataBinder.setDisallowedFields(ID);
    }

    @GetMapping(OWNERS_NEW)
    public String initCreationForm(Map<String, Object> model) {
        Owner owner = new Owner();
        model.put(OWNER, owner);
        return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping(OWNERS_NEW)
    public String processCreationForm(@Valid Owner owner, BindingResult result) {
        if (result.hasErrors()) {
            return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
        } else {
            this.owners.save(owner);
            return REDIRECT_OWNERS + owner.getId();
        }
    }

    @GetMapping(OWNERS_FIND)
    public String initFindForm(Map<String, Object> model) {
        model.put(OWNER, new Owner());
        return OWNERS_FIND_OWNERS;
    }

    @GetMapping(SLASH_OWNERS)
    @Timed(value = LAT_TIME_PROCESS_FIND_FORM)
    public String processFindForm(Owner owner, BindingResult result, Map<String, Object> model) {
        Collection<Owner> results;

        // Increase the lat_requests_total counter metric
	requests.increment();

        // Allow parameterless GET request for /owners to return all records
        if (owner.getLastName() == null) {
            owner.setLastName(""); // Empty string signifies broadest possible search
        }

        // Find owners by last name
        results = this.owners.findByLastName(owner.getLastName());

        try {
          Thread.sleep(5000);
        }
        catch(InterruptedException ex) {
          Thread.currentThread().interrupt();
        }


        if (results.isEmpty()) {
            // No owners found
            result.rejectValue(LAST_NAME, NOT_FOUND, NOT_SPACE_FOUND);
            return OWNERS_FIND_OWNERS;
        } else if (results.size() == 1) {
            // One owner found
            owner = results.iterator().next();
            return REDIRECT_OWNERS + owner.getId();
        } else {
            // Multiple owners found
            model.put(SELECTIONS, results);
            return OWNERS_OWNERS_LIST;
        }
    }

    @GetMapping(OWNERS_OWNER_ID_EDIT)
    public String initUpdateOwnerForm(@PathVariable(OWNER_ID) int ownerId, Model model) {
        Owner owner = this.owners.findById(ownerId);
        model.addAttribute(owner);
        return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
    }

    @PostMapping(OWNERS_OWNER_ID_EDIT)
    public String processUpdateOwnerForm(@Valid Owner owner, BindingResult result, @PathVariable(OWNER_ID) int ownerId) {
        if (result.hasErrors()) {
            return VIEWS_OWNER_CREATE_OR_UPDATE_FORM;
        } else {
            owner.setId(ownerId);
            this.owners.save(owner);
            return "redirect:/owners/{ownerId}";
        }
    }

    /**
     * Custom handler for displaying an owner.
     *
     * @param ownerId the ID of the owner to display
     * @return a ModelMap with the model attributes for the view
     */
    @GetMapping("/owners/{ownerId}")
    public ModelAndView showOwner(@PathVariable("ownerId") int ownerId) {
        ModelAndView mav = new ModelAndView("owners/ownerDetails");
        mav.addObject(this.owners.findById(ownerId));
        return mav;
    }

}
