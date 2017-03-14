#!/usr/bin/env python
import argparse
import logging
import json
import os

logging.basicConfig(
    level=logging.DEBUG,
    format='%(message)s'
)

# import to walk the directory structure, create big json file
def implode(root_dir):
    """ Walks data directory structure to create a big json file """

    # walk the directory tree
    # create big json file
    data = {}
    data['attributes'] = implode_attributes(root_dir)
    data['products'] = implode_products(root_dir, 'products')
    

    data_file = os.path.join('.', 'catalogue_import.json')
    with open(data_file, 'w') as outfile:
        json.dump(data, outfile, sort_keys=True)


def implode_attributes(root_dir):
    """ Returns dict object of attributes """

    # walk attributes directory
    # read attributes.json file
    # return json object
    return json.load(open(os.path.join(root_dir, 'attributes', 'attributes.json')))

def number(s):
    try:
        float(s)
        return True
    except ValueError:
        logging.warning('%s is not a version number in %s', v, d)
        return False

def valid_json(products_dir, d, f):
    try:
        json.load(open(os.path.join(products_dir, d, 'versions', f)))
        return True
    except ValueError, e: 
        logging.warning('Invalid valid json for %s, in %s was %s', f, d, str(e).lower())
        return False

def implode_products(root_dir, data_type):
    """ Returns dict object of products or serviceAddOns """

    # walk attributes directory
    # products
    products_dir = os.path.join(root_dir, data_type)
    # got into each product
    dirs = next(os.walk(products_dir))[1]
    products = {}
    for d in dirs:
        product = {}

        # return timeline
        product['timeline'] = json.load(open(os.path.join(products_dir, d, 'timeline.json')))

        # go into versions dir and return versions
        product['versions'] = {}
        vfiles = next(os.walk(os.path.join(products_dir, d, 'versions')))[2]
        for f in vfiles:
            v = f.split('.')[0]
            extension = f.split('.')[1]
            if extension == 'json':
                if number(v) and valid_json(products_dir, d, f):
                    product['versions'][v] = json.load(open(os.path.join(products_dir, d, 'versions', f)))       
            else: 
                logging.warning('%s in %s has incorrect file type. This needs to be a json file', f, d)

        products[d] = product

    # return json object
    return products


# export endpoint to create directory structure from big json file
def explode(root_dir, json_file):
    """ Creates directory structure from big json file """

    if not os.path.exists(root_dir):
        os.mkdir(root_dir)
    try:
        # get json file
        # parse json file
        json_data = json.load(open(json_file))
        # create directory structure
        create_attributes_dir_structure(root_dir, json_data)
        create_products_dir_structure(root_dir, json_data, 'products')
    except IOError as e:
        raise e

    return True


def create_attributes_dir_structure(root_dir, json_data):
    """ Creates directory structure for attributes object """

    # create attributes directory under 'root_dir'
    attr_dir = os.path.join(root_dir, 'attributes')
    attr_file = os.path.join(attr_dir, 'attributes.json')
    if not os.path.exists(attr_dir):
        os.mkdir(attr_dir)
    # dump json_data['attributes'] into attributes/attributes.json
    with open(attr_file, 'w') as outfile:
        json.dump(json_data['attributes'], outfile, indent=4, sort_keys=True)

    return True


def create_products_dir_structure(root_dir, json_data, data_type):
    """ Creates directory structure for products or serviceAddOns objects """

    # create products directory under 'root_dir'
    data_type_dir = os.path.join(root_dir, data_type)
    if not os.path.exists(data_type_dir):
        os.mkdir(data_type_dir)

    # create a directory per product
    for p in json_data[data_type].keys():
        pd = os.path.join(data_type_dir, p)
        if not os.path.exists(pd):
            os.mkdir(pd)

        # create timeline.json & versions dir under product dir
        timeline_file = os.path.join(pd, 'timeline.json')
        with open(timeline_file, 'w') as outfile:
            json.dump(json_data[data_type][p]['timeline'], outfile, indent=4, sort_keys=True)

        versions_dir = os.path.join(pd, 'versions')
        if not os.path.exists(versions_dir):
            os.mkdir(versions_dir)

        # create version.json files under versions dir
        versions = json_data[data_type][p]['versions']
        for v in versions.keys():
            vf = os.path.join(versions_dir, v + '.json')
            with open(vf, 'w') as outfile:
                json.dump(versions[v], outfile, indent=4, sort_keys=True)

    return True


##################
## Args Parsing ##
##################
parser = argparse.ArgumentParser(description='Catalogue Import-Export Script.')
parser.add_argument('-d', '--dir', metavar='DIRECTORY', dest='data_dir', required=True,
        help='Directory of catalogue data to walk or create')
parser.add_argument('-f', '--file', metavar='DATAFILE', dest='data_file',
        help='Catalogue data json file (a.k.a big json file)')
group = parser.add_mutually_exclusive_group()
group.add_argument('-e', '--explode', action='store_true', dest='explode',
        help='Create catalogue data directory structure from big json file')
group.add_argument('-i', '--implode', action='store_true', dest='implode',
        help='Opposite of explode - walk directory structure and create big json file')

args = parser.parse_args()

if __name__ == '__main__':
    logging.debug(args)
    if args.explode:
        explode(args.data_dir, args.data_file)
    if args.implode:
        implode(args.data_dir)


